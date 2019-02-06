(ns clj-insim.socket
  (:require [clojure.java.io :as io]
            [clj-insim.packets :as packets]
            [clj-insim.enums :as enums]
            [clj-insim.util :as util]
            [clj-insim.parser :refer [parse]])
  (:import [java.net Socket]))

(def ^:private HOST "127.0.0.1") ;; Default host
(def ^:private PORT 29999) ;; Default port
(def ^:private INTERVAL 15) ;; Default update interval

(defn- split-packets [result coll]
  (if-not (seq coll)
    result
    (let [length (first coll)]
      (recur (conj result (into [] (doall (take length coll)))) (drop length coll)))))

(defn- write-multi [output-stream packets]
  (.flush
   (reduce
    (fn [os packet]
      (doto os
        (.write packet)))
    output-stream packets)))

;;;;; PUBLIC FUNCTIONS ;;;;;

(defn client [handler {:keys [host port interval debug]}]
  (let [running (atom true)]
    (future
      (with-open [socket (Socket. (or host "127.0.0.1") (or port 29999))
                  input-stream (io/input-stream socket)
                  output-stream (write-flush (io/output-stream socket) (packets/is-isi))]
        (while @running
          (if (pos? (.available input-stream))
            (let [bytearray (byte-array (.available input-stream))
                  bytes (.read input-stream bytearray)
                  data (split-packets [] (map util/->unsigned-byte bytearray))
                  returns (map #(handler (parse %)) data)]
              (when debug
                (println (str "Received: " (clojure.string/join ", " (doall (map #(name (enums/isp-key (second %))) data))))))
              (doseq [nil-or-coll returns] ;; Returns is a coll with handled values (nil or coll)
                (when-let [packet-colls (seq (remove nil? nil-or-coll))]
                  (write-multi output-stream packet-colls)
)))
            (Thread/sleep (or interval 150))))))
    running))
