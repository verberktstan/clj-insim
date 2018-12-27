(ns clj-insim.socket
  (:require [clojure.java.io :as io]
            [clj-insim.packets :as packets]
            [clj-insim.util :refer [->unsigned-byte]])
  (:import [java.net Socket]))

(def HOST "127.0.0.1")
(def PORT 29999)

(defn- split-packets [result coll]
  (if-not (seq coll)
    result
    (let [length (first coll)]
      (recur (conj result (take length coll)) (drop length coll)))))

(defn- receive-packets [socket]
  (let [in (io/input-stream socket)
        available (.available in)]
    (when (pos? available)
      (let [ba (byte-array available)]
        (.read in ba)
        (split-packets nil (map ->unsigned-byte ba))))))

(defn- send-packets [socket packets]
  (let [out (io/output-stream socket)]
    (if (coll? packets)
      (.write out (byte-array (mapcat seq (flatten packets))))
      (.write out packets))
    (.flush out)))

;;;;; PUBLIC FUNCTIONS ;;;;;

(defn client [handler]
  (let [running (atom true)]
    (future
      (with-open [socket (Socket. HOST PORT)
                  _ (send-packets socket (packets/is-isi))]
        (while @running
          (let [in (receive-packets socket)
                out (when in (handler in))]
            (when out (send-packets socket out))))))
    running))

(comment
  (defn test-handler [p]
    (do
      (newline)
      (println (str p))
      (packets/is-tiny {:reqi 0})))

  (def lfs (client test-handler))
  (reset! lfs false)
)
