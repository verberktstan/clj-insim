(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parsers :refer [parse]]
            [clj-insim.socket :refer [client]]
            [clj-insim.util :as util])
  (:import [java.nio ByteBuffer]))

(def connections (atom {})) ;; @connections
(def players (atom {})) ;; @players
(def race-in-progress? (atom :none))

(def championship
  [{:player-name "AI 1" :points 10}
   {:player-name "AI 2" :points 12}
   {:player-name "AI 3" :points 8}
   {:player-name "Henk" :points 7}
   {:player-name "AI 5" :points 6}
   {:player-name "AI 6" :points 11}
   {:player-name "AI 8" :points 1}])

(defn positional-ballast [i]
  (case i
    0 54
    1 48
    2 42
    3 36
    4 30
    5 24
    6 18
    7 12
    8 6
    9 6
    0))

(defn calculate-ballast
  "Calculate success ballast based on position of player in result."
  [result]
  (let [result (sort-by :points > result)]
    (map-indexed
     (fn [i player]
       (assoc player :handicap-mass (positional-ballast i)))
     result)))

(def success-ballast (calculate-ballast championship)) ;; success-ballast

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

(defn respects-handicaps? [{:keys [player-name handicap-mass handicap-restriction]} success-ballast]
  (let [p (first (filter #(= (:player-name %) player-name) success-ballast))]
    (and
     (or (not (:handicap-mass p))
         (>= handicap-mass (:handicap-mass p)))
     (or (not (:handicap-restriction p))
         (>= handicap-restriction (:handicap-restriction p))))))

(defn reject [{:keys [uniq-connection-id] :as npl}]
  (packets/is-jrr {:uniq-connection-id uniq-connection-id
                   :jrr-action (enums/jrr-action :reject)}))

(defn spawn [{:keys [player-name uniq-connection-id] :as npl}]
  (packets/is-jrr {:uniq-connection-id uniq-connection-id
                   :jrr-action (enums/jrr-action :spawn)}))

(defn verify-new-player-join [{:keys [number-player] :as new-player}]
  (when (= number-player 0) ; If this is a join request...
    (if (respects-handicaps? new-player success-ballast)
      (spawn new-player)
      (reject new-player))))

(defn register-connection [{:keys [uniq-connection-id] :as new-connection}]
  (swap! connections assoc (key uniq-connection-id)
         (select-keys new-connection [:user-name :player-name :uniq-connection-id :admin :total :flags])))

(defn new-connection [{:keys [player-name reqi] :as ncn}]
  (when (= reqi 0) ; If new connection (not a response to info request)
    (let [{:keys [handicap-mass]} (first (filter #(= (:player-name %) player-name) success-ballast))]
      (register-connection ncn)
      (packets/is-mst
       (str player-name "'s current succest ballast: " (if handicap-mass handicap-mass 0) "kg")))))

(defn update-state [{:keys [race-in-progress]}]
  (when (not= @race-in-progress? race-in-progress)
    (do
      (reset! race-in-progress? race-in-progress)
      (packets/is-mst (str (name race-in-progress) " started!")))))

(def dispatchers
  {:ncn new-connection
   :npl verify-new-player-join
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
