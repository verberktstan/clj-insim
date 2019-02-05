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

(defn- write-flush [output-stream packet]
  (doto output-stream
    (.write packet)
    .flush))

;;;;; PUBLIC FUNCTIONS ;;;;;

(defn client [handler & {:keys [host port interval]}]
  (let [running (atom true)]
    (future
      (with-open [socket (Socket. (or host HOST) (or port PORT))
                  input-stream (io/input-stream socket)
                  output-stream (write-flush (io/output-stream socket) (packets/is-isi))]
        (while @running
          (if (pos? (.available input-stream))
            (let [bytearray (byte-array (.available input-stream))
                  bytes (.read input-stream bytearray)
                  data (split-packets [] (doall (map util/->unsigned-byte bytearray)))
                  returns (doall (map handler data))]
              (doseq [packet returns]
                (if (coll? packet)
                  (doseq [sub-packet (remove nil? packet)]
                    (write-flush output-stream sub-packet))
                  (write-flush output-stream packet))))
            (Thread/sleep (or interval INTERVAL))))))
    running))

(comment
  (defmulti packet-dispatch :type)
  (defmethod packet-dispatch :ver
    (packets/is-msl "Warm welcome from clj-insim!"))
  (defmethod packet-dispatch :mso [_]
    (packets/is-hcp {:xrt {:restriction 2} :fxo {:restriction 5} :uf1 {:restriction 10}}))
  (defmethod packet-dispatch :default [p] nil)

  (defn handler [p]
    (let [packet (parse p)]
      (newline)
      (prn packet)
      (or (packet-dispatch packet)
       (packets/is-tiny))))

  (def lfs (client handler))
  (reset! lfs false)

  (parse [28 17 0 0 1 0 2 65 82 79 53 0 0 0 0 1 97 1 190 0 58 0 182 0 255 255 255 255])
  (parse [4 23 0 1])

  (def test-data (read-string (slurp "race-data.txt")))
  (parse (nth test-data 3))
  (handler (nth test-data 3))
)
