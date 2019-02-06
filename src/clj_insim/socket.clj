(ns clj-insim.socket
  (:require [clojure.java.io :as io]
            [clj-insim.packets :as packets]
            [clj-insim.enums :as enums]
            [clj-insim.util :as util]
            [clj-insim.parser :refer [parse]])
  (:import [java.net Socket]))

(defn- split-packets [result coll]
  (if-not (seq coll)
    result
    (let [length (first coll)]
      (recur (conj result (into [] (doall (take length coll)))) (drop length coll)))))

(defn- write-flush [output-stream byte-array]
  (doto output-stream
    (.write byte-array)
    .flush))

(defn concat-byte-arrays [& byte-arrays]
  (when (not-empty byte-arrays)
    (let [total-size (reduce + (map count byte-arrays))
          result (byte-array total-size)
          bb (java.nio.ByteBuffer/wrap result)]
      (doseq [ba byte-arrays]
        (.put bb ba))
      result)))

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
                (println (str "Received: " (clojure.string/join ", " (mapv #(name (enums/isp-key (second %))) data)))))
              (doseq [nil-or-coll returns] ;; Returns is a coll with handled values (nil or coll)
                (when-let [packet-colls (seq (remove nil? nil-or-coll))]
                  (write-flush output-stream (apply concat-byte-arrays packet-colls))
)))
            (Thread/sleep (or interval 150))))))
    running))
