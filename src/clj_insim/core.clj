(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parsers :refer [parse]]
            [clj-insim.socket :refer [client]]
            [clj-insim.util :as util])
  (:import [java.nio ByteBuffer]))

(def CAR-HANDICAPS
  {"XFG" [{:mass 30} ; first (0)
          {:mass 27}
          {:mass 24}
          {:mass 21}
          {:mass 18}
          {:mass 15}
          {:mass 12}
          {:mass 9}
          {:mass 6} ; ninth (8)
          {:mass 3}]
   "XRG" [{:mass 27 :restriction 1} ; first (0)
          {:mass 24 :restriction 1}
          {:mass 21 :restriction 1}
          {:mass 21}
          {:mass 18}
          {:mass 15}
          {:mass 12}
          {:mass 9}
          {:mass 6} ; ninth (8)
          {:mass 3}]})

(def connections (atom {})) ;; @connections
(def players (atom {})) ;; @players
(def rejected (atom [])) ;; @rejected
(def race-in-progress? (atom :none))

(def championship
  (sort-by :points > [{:player-name "AI 1" :points 10}
                      {:player-name "AI 2" :points 12}
                      {:player-name "AI 3" :points 8}
                      {:player-name "Henk" :points 13}
                      {:player-name "AI 5" :points 6}
                      {:player-name "AI 6" :points 11}
                      {:player-name "AI 8" :points 1}]))
;; (assoc-positions championship)

;; Basic handicap mass is (horsepower/kilograms) * 250
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
  (let [result (assoc-positions championship)
        {:keys [position]} (first (filter #(= (:player-name %) player-name) result))]
    (when position
      (car-handicaps car-name position))))
;; (handicaps-for-player "Henk" "XRG")
;; (handicaps-for-player "AI 2" "XFG")
;; (handicaps-for-player "AI 3" "XFG")
;; (handicaps-for-player "AI 4" "XRG")

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
    (if (respect-handicaps? player-name car-name handicap-mass handicap-restriction)
      (spawn new-player)
      (reject new-player))))

(defn update-state [{:keys [race-in-progress]}]
  (when (not= @race-in-progress? race-in-progress)
    (do
      (reset! race-in-progress? race-in-progress)
      (packets/is-mst (str (name race-in-progress) " started!")))))

;; Specify dispatchers for each type of packet
(def dispatchers
  {
;:mso reply-messages
;   :ncn new-connection
   :npl new-player
   :sta update-state
   :tiny dispatch-tiny
   :ver check-version})

(defn dispatch [{:keys [type] :as incoming}]
  (when incoming
    (when-let [f (type dispatchers)]
      (f incoming))))

(defn print-incoming [type-key incoming]
  (do
    (println "\n-== Received " (name type-key) " packet from LFS ==-")
    (prn incoming)))

(defn handler [[type :as packet]]
  (let [type-key (enums/isp-key type)]
    (when-let [incoming (parse type-key packet)]
      (print-incoming type-key incoming)
      (or (dispatch incoming) (packets/is-tiny)))))

(comment
  ;; Start a tcp client with a handler
  (def simple-client (client handler))
  ;; To stop the client
  (reset! simple-client false)
)
