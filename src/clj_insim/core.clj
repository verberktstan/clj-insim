(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parsers :refer [parse]]
            [clj-insim.socket :refer [client]]
            [clj-insim.util :as util]))

(defonce race-in-progress? (atom :none))

(defn- welcome
  "Returns an IS_MST packet with a warm welcom message from clj-insim."
  []
  (packets/is-mst "Hello from clj-insim!"))

(defn- check-version
  "Returns a welcome message if LFS version of host is 0.6T and insim-version is greater than 7. Else returns an IS_TINY packet that closes the connection."
  [{:keys [version insim-version]}]
  (if (and (= version "0.6T")
           (>= insim-version 7))
    (welcome)
    (packets/is-tiny {:data-key :close})))

(defn- update-state
  "Returns an IS_MST packet with a notification of the changed race-in-progress state. If race-in-progress is not changed, returns nil."
  [{:keys [race-in-progress]}]
  (when (not= @race-in-progress? race-in-progress)
    (do
      (reset! race-in-progress? race-in-progress)
      (packets/is-mst (str (name race-in-progress) " started!")))))

;; Specify dispatchers for each type of packet
(def ^:private dispatchers
  {:sta update-state
   :ver check-version})

(defn- dispatch
  "Returns the result of (dispachters (:type packet)) applied to the incoming packet. Prints incoming packet as side effect."
  [{:keys [type] :as packet}]
  (when packet
    (println (str "\n== clj-insim received a " (name type) " packet from LFS =="))
    (prn packet)
    (when-let [f (type dispatchers)]
      (f packet))))

(defn- test-handler
  "Consumes a binary packet from LFS InSim, returns a packet by dispatching. If dispatch returns nil, will return a IS_TINY packet to maintain connection."
  [packet]
  (or (-> packet parse dispatch) (packets/is-tiny)))

(defn start-test-client []
  (client test-handler))

(comment
  (def lfs-client (start-test-client))
  (reset! lfs-client false)
)
