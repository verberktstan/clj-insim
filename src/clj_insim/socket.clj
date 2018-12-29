(ns clj-insim.socket
  (:require [clojure.java.io :as io]
            [clj-insim.packets :as packets]
            [clj-insim.enums :as enums]
            [clj-insim.util :refer [->unsigned-byte]])
  (:import [java.net Socket]))

(def ^:private HOST "127.0.0.1") ;; Default host
(def ^:private PORT 29999) ;; Default port
(def ^:private INTERVAL 50) ;; Default update interval

(defn- split-packets [result coll]
  (if-not (seq coll)
    result
    (let [length (first coll)]
      (recur (conj result (into [] (doall (take length coll)))) (drop length coll)))))

;;;;; PUBLIC FUNCTIONS ;;;;;

(defn client [handler & {:keys [host port interval]}]
  (let [running (atom true)]
    (future
      (with-open [socket (Socket. (or host HOST) (or port PORT))
                  input-stream (io/input-stream socket)
                  output-stream (doto (io/output-stream socket)
                                  (.write (packets/is-isi))
                                  .flush)]
        (while @running
          (if (pos? (.available input-stream))
            (let [bytearray (byte-array (.available input-stream))
                  bytes (.read input-stream bytearray)
                  data (split-packets [] (map ->unsigned-byte bytearray))
                  _ (handler data)]
              (doto output-stream
                (.write (packets/is-tiny))
                .flush))
            (Thread/sleep (or interval INTERVAL))))))
    running))

;; (def packets (atom []))

(def lfs (client prn))
;; (def lfs (client #(swap! packets conj %)))
(reset! lfs false)
