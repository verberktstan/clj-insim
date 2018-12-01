(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parsers :refer [parse-packet]]
            [clj-insim.protocols :refer [is-protocols]]
            [clj-insim.socket :refer [serve]]
            [clj-insim.util :as util])
  (:import [java.nio ByteBuffer]))

(def success-mass [{:player-name "Van Sterberkt" :handicap-mass 10}
                   {:player-name "Boer Tarrel" :handicap-mass 15}])

(defn welcome []
  (packets/is-mst "Hello from clj-insim!"))

(defn close-connection []
  (packets/is-tiny {:data-key :close}))

(defn check-version [{:keys [version insim-version]}]
  (if (and (= version "0.6T")
           (>= insim-version 7))
    (welcome)
    (close-connection)))

(defn dispatch-tiny [{:keys [subt]}]
  (do
    (println "Sent IS_TINY to maintain connection...")
    (packets/is-tiny)))

(defn respects-handicaps? [{:keys [player-name handicap-mass handicap-restriction]}]
  (let [p (first (filter #(= (:player-name %) player-name) success-mass))]
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
    (if (respects-handicaps? npl)
      (spawn uniq-connection-id)
      (reject uniq-connection-id))
    (packets/is-tiny)))

(def dispatchers
  {:npl verify-new-player-join
   :tiny dispatch-tiny
   :ver check-version})

(defn dispatch [{:keys [type] :as incoming}]
  (let [f (type dispatchers)]
    (do (prn incoming)
      (if f
        (f incoming)
        (packets/is-tiny)))))

(defn parse [type-key packet]
  (parse-packet packet (is-protocols type-key)))

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

  (:test nil)
)
