(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parsers :as parsers]
            [clj-insim.socket :refer [client]]
            [clj-insim.util :as util]))

(def results (atom {}))

(defn- check-version
  "Returns a welcome message if LFS version of host is 0.6T and insim-version is greater than 7. Else returns an IS_TINY packet that closes the connection."
  [{:keys [version insim-version]}]
  (if (and (= version "0.6T")
           (>= insim-version 7))
    (packets/is-mst "HELLOOO!")
    (packets/is-tiny {:data-key :close})))

(defn- consume-result [{:keys [player-name result-num] :as result}]
  (do
    (swap! results assoc result-num result)
    (packets/is-mtc 255 0 (str player-name " finished in place " result-num))))

;; Specify dispatchers for each type of packet
(def ^:private dispatchers
  {:ver check-version
   :res consume-result})

(defn- dispatch
  "Returns the result of (dispachters (:type packet)) applied to the incoming packet. Prints incoming packet as side effect."
  [{:keys [type] :as packet}]
  (when packet
    (prn packet)
    (when-let [f (type dispatchers)]
      (f packet))))

;;;;; public functions

(defn parse
  ([packet]
   (let [type-key (-> packet first enums/isp-key)]
     (parse type-key packet)))
  ([type-key packet]
   (let [protocol (parsers/make-protocol type-key)]
     (when protocol
       (reduce parsers/parse-bytes {:coll packet} protocol)))))

(defn test-handler
  "Consumes packets from LFS InSim."
  [packets]
  (doall
   (map #(do (println (str "\n== clj-insim received a " (-> % first enums/isp-key name) " packet from LFS =="))
             (or (-> % parse dispatch) (packets/is-tiny)))
        packets)))

(defn handler [packets]
  (do
    (doseq [packet packets]
      (println "\nclj-insim received a packet from lfs:")
      (prn packet)))
  (packets/is-tiny))

(comment
  (def lfs-client (client handler))
  (reset! lfs-client false)
)
