(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parsers :as parsers]
            [clj-insim.socket :refer [client]]
            [clj-insim.util :as util]))

(defn- check-version
  "Returns a welcome message if LFS version of host is 0.6T and insim-version is greater than 7. Else returns an IS_TINY packet that closes the connection."
  [{:keys [version insim-version]}]
  (if (and (= version "0.6T")
           (= insim-version 8))
    (packets/is-msl "Warm welcome from clj-insim!")
    (packets/is-tiny {:data-key :close})))

;; Specify dispatchers for each type of packet
(def ^:private dispatchers
  {:ver check-version})

;;;;; public functions

(defn dispatch
  "Returns the result of (dispachters (:type packet)) applied to the incoming packet. Prints incoming packet as side effect."
  ([packet]
   (dispatch nil))
  ([packet options]
   (dispatch dispatchers packet options))
  ([dispatchers {:keys [type] :as packet} {:keys [debug]}]
   (when packet
     (when debug (prn packet))
     (when-let [f (type dispatchers)]
       (f packet)))))

(defn parse
  ([packet]
   (let [type-key (-> packet second enums/isp-key)]
     (parse type-key packet)))
  ([type-key packet]
   (let [protocol (parsers/make-protocol type-key)]
     (if protocol
       (reduce parsers/parse-bytes {:coll packet} protocol)
       {:type type-key :packet packet}))))

(defn handler
  "Parse packet, print notification and call dispatch fn OR return the IS_TINY keepalive packet."
  [packet]
  (let [{:keys [type] :as parsed} (parse packet)]
    (println (str "\n== Received a " (name type) " packet from LFS =="))
    (or (dispatch dispatchers parsed {:debug true}) (packets/is-tiny))))

(defn print-handler
  "Print a (parsed) packet and return the IS_TINY keepalive packet"
  [packet]
  (let [{:keys [type] :as parsed} (parse packet)]
    (println (str "\n== Received a " (name type) " packet from LFS =="))
    (prn parsed)
    [(packets/is-tiny {:reqi 0 :data-key :none})]))

(comment
  (def lfs-client (client handler))
  (reset! lfs-client false)

)
