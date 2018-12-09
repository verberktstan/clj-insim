(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parsers :refer [parse]]
            [clj-insim.socket :refer [client]]
            [clj-insim.util :as util])
  (:import [java.nio ByteBuffer]))

(def POWER-WEIGHT {"XFG" {:power 115 :weight 942}
                   "XRG" {:power 140 :weight 1150}
                   "RB4" {:power 243 :weight 1210}
                   "FXO" {:power 234 :weight 1136}
                   "XRT" {:power 247 :weight 1223}
                   "LX4" {:power 140 :weight 499}
                   "LX6" {:power 190 :weight 539}
                   "RAC" {:power 245 :weight 800}
                   "FZ5" {:power 360 :weight 1380}})

(defn positional-handicap-mass [{:keys [power weight]}]
  (let [base (int (* (/ power weight) 180))]
    (map #(int (* base %)) [0.9 0.8 0.7 0.6 0.5 0.4 0.3 0.2 0.1 0.1])))

(def CAR-NAMES ["XFG" "XRG" "RB4" "FXO" "XRT"])

(defn car-masses [car-name]
  (map #(assoc {} :mass %) (positional-handicap-mass (power-weight car-name))))

(def CAR-HANDICAPS
  (reduce #(assoc %1 %2 (car-masses %2)) {} (keys POWER-WEIGHT)))

(defonce connections (atom {})) ;; @connections
(defonce players (atom {})) ;; @players
(defonce rejected (atom [])) ;; @rejected
(defonce race-in-progress? (atom :none))

(defonce championship (atom [{:player-name "Henk" :points 0}])) ;; @championship

(defn known-player? [player-name championship]
  (some #(= (:player-name %) player-name) championship))

(defn new-championship-player! [uniq-connection-id player-name championship]
  (when (not (known-player? player-name @championship))
    (swap! championship conj {:player-name player-name :uniq-connection-id uniq-connection-id :points 0 :races-finished 0})))

(defn car-handicaps
  "Returns handicaps for position i for a specific car name"
  [car-name i]
  (-> CAR-HANDICAPS
      (get car-name)
      (nth i)))

;; (car-handicaps "XFG" 0)

(defn assoc-positions [result]
  (map-indexed
   (fn [i player]
     (assoc player :position i))
   (sort-by :points > result)))
;; (assoc-positions championship)

(defn handicaps-for-player [player-name car-name]
  (let [result (assoc-positions @championship)
        {:keys [position]} (first (filter #(= (:player-name %) player-name) result))]
    (when position
      (car-handicaps car-name position))))
;; (handicaps-for-player "Henk" "XRG")
;; (handicaps-for-player "AI 2" "XFG")
;; (handicaps-for-player "AI 3" "XFG")
;; (handicaps-for-player "AI 4" "XRG")
;; (handicaps-for-player "AI 10" "XRT")

(defn welcome []
  (packets/is-mst "Hello from clj-insim!"))

(defn close-connection []
  (packets/is-tiny {:data-key :close}))

(defn check-version [{:keys [version insim-version]}]
  (if (and (= version "0.6T")
           (>= insim-version 7))
    (welcome)
    (close-connection)))

(defn dispatch-tiny [{:keys [sub-type]}]
  (do
    (println "Sent IS_TINY to maintain connection...")
    (packets/is-tiny)))

(defn reject [{:keys [uniq-connection-id] :as npl}]
  (packets/is-jrr {:uniq-connection-id uniq-connection-id
                   :jrr-action (enums/jrr-action :reject)}))

(defn spawn [{:keys [uniq-connection-id] :as npl}]
  (packets/is-jrr {:uniq-connection-id uniq-connection-id
                   :jrr-action (enums/jrr-action :spawn)}))

(defn respect-handicaps? [player-name car-name handicap-mass handicap-restriction]
  (let [{:keys [mass restriction]} (handicaps-for-player player-name car-name)]
    (and
     (or (nil? mass) (>= handicap-mass mass))
     (or (nil? restriction) (>= handicap-restriction restriction)))))

(defn new-player [{:keys [car-name uniq-connection-id player-name handicap-mass handicap-restriction number-player] :as new-player}]
  (when (= number-player 0) ; When this is a join request...
    (new-championship-player! uniq-connection-id player-name championship)
    (if (respect-handicaps? player-name car-name handicap-mass handicap-restriction)
      (spawn new-player)
      (reject new-player))))

(defn update-state [{:keys [race-in-progress]}]
  (when (not= @race-in-progress? race-in-progress)
    (do
      (reset! race-in-progress? race-in-progress)
      (packets/is-mst (str (name race-in-progress) " started!")))))

(defn consume-result [{:keys [reqi]}]
  (when (= reqi 0)
    (packets/is-tiny :res)))

(defn request-result [lap]
  (packets/is-tiny :res))

(defn print-handicaps [uniq-connection-id player-id]
  (packets/is-mst "Showing your handicaps!"))

(defn message-out [{:keys [text-start message user-type player-id uniq-connection-id]}]
  (when (= user-type :prefix)
    (let [command (subs message text-start)]
      (println "Player " player-id " => " command)
      (case command
        "!handicaps" (print-handicaps uniq-connection-id player-id)
        nil))))

;; Specify dispatchers for each type of packet
(def dispatchers
  {:lap request-result
   :mso message-out
   :npl new-player
   :res consume-result
   :sta update-state
   :tiny dispatch-tiny
   :ver check-version})

(defn dispatch [{:keys [type] :as incoming}]
  (when incoming
    (when-let [f (type dispatchers)]
      (f incoming))))

(defn handler [[type :as packet]]
  (let [type-key (enums/isp-key type)]
    (println "\n-== Received " (name type-key) " packet from LFS ==-")
    (if-let [incoming (parse type-key packet)]
      (do
        (prn incoming)
        (or (dispatch incoming) (packets/is-tiny)))
      (packets/is-tiny))))

(comment
  ;; Start a tcp client with a handler
  (def simple-client (client handler))
  ;; To stop the client
  (reset! simple-client false)
)
