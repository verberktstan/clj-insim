(ns clj-insim.core
  (:require [clj-insim.cars :refer [car-handicaps]]
            [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parsers :refer [parse]]
            [clj-insim.socket :refer [client]]
            [clj-insim.util :as util]))

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

(defn update-state [{:keys [race-in-progress]}]
  (when (not= @race-in-progress? race-in-progress)
    (do
      (reset! race-in-progress? race-in-progress)
      (packets/is-mst (str (name race-in-progress) " started!")))))

;; Specify dispatchers for each type of packet
(def dispatchers
  {:sta update-state
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
