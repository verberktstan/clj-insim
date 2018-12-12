(ns clj-insim.core
  (:require [clj-insim.cars :refer [car-handicaps]]
            [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parsers :refer [parse]]
            [clj-insim.socket :refer [client]]
            [clj-insim.util :as util]))

(defonce connections (atom {})) ;; @connections
;; (reset! connections {})
(defonce players (atom {})) ;; @players
;; (reset! players {})
(defonce race-in-progress? (atom :none))

(defonce championship (atom [{:player-name "Henk" :points 0}])) ;; @championship

(defn known-player? [player-name championship]
  (some #(= (:player-name %) player-name) championship))

(defn new-connection! [uniq-connection-id player-name]
  (when (not (contains? @connections uniq-connection-id))
    (swap! connections assoc uniq-connection-id player-name)))

(defn new-championship-player! [uniq-connection-id player-name championship]
  (when (not (known-player? player-name @championship))
    (swap! championship conj {:player-name player-name :uniq-connection-id uniq-connection-id :points 0 :races-finished 0})))

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


(defn register-player! [{:keys [uniq-connection-id player-id] :as player}]
  (do
    (when (not (contains? @connections uniq-connection-id))
      (swap! connections assoc uniq-connection-id true)
      (println (str "Connection " uniq-connection-id " registered!")))
    (when (not (contains? @players player-id))
      (swap! players assoc player-id player)
      (println (str "Player " player-id " joined!")))))

(defn unregister-player! [{:keys [player-id]}]
  (when (contains? @players player-id)
    (swap! players dissoc player-id)
    (println (str "Player " player-id " left!"))))

(defn update-state [{:keys [race-in-progress]}]
  (when (not= @race-in-progress? race-in-progress)
    (do
      (reset! race-in-progress? race-in-progress)
      (packets/is-mst (str (name race-in-progress) " started!")))))

(defn message-out [{:keys [text-start message user-type player-id uniq-connection-id]}]
  (when (= user-type :prefix)
    (let [command (subs message text-start)]
      (println "Player " player-id " => " command)
      (case command
        "!handicaps" (packets/is-mst "Print handicaps!")
        nil))))

;; Specify dispatchers for each type of packet
(def dispatchers
  {:npl register-player!
   :pll unregister-player!
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
    (prn packet)
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
