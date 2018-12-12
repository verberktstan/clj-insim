(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parsers :refer [parse]]
            [clj-insim.socket :refer [client]]
            [clj-insim.util :as util]))

(defonce race-in-progress? (atom :none))

(defn- welcome []
  (packets/is-mst "Hello from clj-insim!"))

(defn- close-connection []
  (packets/is-tiny {:data-key :close}))

(defn- check-version [{:keys [version insim-version]}]
  (if (and (= version "0.6T")
           (>= insim-version 7))
    (welcome)
    (close-connection)))

(defn- update-state [{:keys [race-in-progress]}]
  (when (not= @race-in-progress? race-in-progress)
    (do
      (reset! race-in-progress? race-in-progress)
      (packets/is-mst (str (name race-in-progress) " started!")))))

;; Specify dispatchers for each type of packet
(def ^:private dispatchers
  {:sta update-state
   :ver check-version})

(defn- dispatch [{:keys [type] :as incoming}]
  (when incoming
    (when-let [f (type dispatchers)]
      (f incoming))))

(defn- default-handler [packet]
  (if-let [{:keys [type] :as parsed-packet} (parse packet)]
    (if-let [dispatch-fn (dispatch type)]
      (dispatch-fn parsed-packet)
      (packets/is-tiny))
    (packets/is-tiny)))

(defn start-client
  ([]
   (client default-handler))
  ([host]
   (client default-handler {:host host}))
  ([host port]
   (client default-handler {:host host :port port})))
