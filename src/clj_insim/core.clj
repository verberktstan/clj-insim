(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parsers :refer [parse]]
            [clj-insim.socket :refer [serve]]
            [clj-insim.util :as util])
  (:import [java.nio ByteBuffer]))

(def connections (atom {}))
(def players (atom {}))

(def championship
  [{:player-name "AI 1" :points 10}
   {:player-name "AI 2" :points 12}
   {:player-name "AI 3" :points 8}
   {:player-name "AI 5" :points 6}
   {:player-name "AI 6" :points 11}
   {:player-name "AI 8" :points 1}])

(defn positional-ballast [i]
  (case i
    0 50
    1 44
    2 38
    3 32
    4 26
    5 21
    6 15
    7 8
    0))

(defn calculate-ballast
  "Calculate success ballast based on position of player in result."
  [result]
  (let [result (sort-by :points > result)]
    (map-indexed
     (fn [i player]
       (assoc player :handicap-mass (positional-ballast i)))
     result)))

(def success-ballast (calculate-ballast championship))
;; success-ballast

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

(defn reject [uniq-connection-id]
  (packets/is-jrr {:uniq-connection-id uniq-connection-id
                   :jrr-action (enums/jrr-action :reject)}))
(defn spawn [uniq-connection-id]
  (packets/is-jrr {:uniq-connection-id uniq-connection-id
                   :jrr-action (enums/jrr-action :spawn)}))

(defn verify-new-player-join [{:keys [number-player uniq-connection-id] :as npl}]
  (if (= number-player 0) ; If this is a join request...
    (if (respects-handicaps? npl success-ballast)
      (spawn uniq-connection-id)
      (reject uniq-connection-id))
    (packets/is-tiny)))

(defn new-connection [{:keys [player-name reqi] :as ncn}]
  (if (= reqi 0) ; If new connection (not a response to info request)
    (let [{:keys [handicap-mass]} (first (filter #(= (:player-name %) player-name) success-ballast))]
      (do
        (swap! connections assoc player-name (select-keys ncn [:user-name :player-name :uniq-connection-id]))
        (packets/is-mst
         (str player-name "'s current succest ballast: " (if handicap-mass handicap-mass 0) "kg"))))
    (packets/is-tiny)))

(def dispatchers
  {:ncn new-connection
   :npl verify-new-player-join
   :tiny dispatch-tiny
   :ver check-version})

(defn dispatch [{:keys [type] :as incoming}]
  (let [f (type dispatchers)]
    (do (prn incoming)
      (if f
        (f incoming)
        (packets/is-tiny)))))

;; Simple hander prints the type of packet received and returns a IS_TYNI/none packet to maintain connection
(defn handler [packet]
  (let [[type] packet
        type-key (enums/isp-key (int type))
        incoming (parse type-key packet)]
    (do
      (println "\n=== Received " (name type-key) " packet from LFS ===")
      (if incoming
        (dispatch incoming)
        (do (println "Incomping packet cannot be parsed: sending a IS_TINY packet by default...")
          (packets/is-tiny))))))

(comment
  ;; Start a tcp client with simple-handler
  (def simple-server (serve handler))
  ;; To stop the client
  (reset! simple-server false)
)
