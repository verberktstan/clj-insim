(ns clj-insim.socket
  (:require [clojure.java.io :as io]
            [clj-insim.packets :as packets]
            [clj-insim.util :refer [->unsigned-byte]])
  (:import [java.net Socket]))

(def HOST "127.0.0.1")
(def PORT 29999)

(defn- collect-bytes [result in]
  (if (pos? (.available in))
    (let [size (.read in)
          ba (byte-array (dec size))
          _ (.read in ba)]
      (conj (collect-bytes result in) (doall (map ->unsigned-byte (seq ba)))))
    result))

(defn- receive-packets [socket]
  (let [in (io/input-stream socket)]
    (collect-bytes '() in)))

(defn- send-packets
  "Send packet(s) to socket."
  [socket packets]
  (let [out (io/output-stream socket)]
    (if (coll? packets)
      (.write out (byte-array (mapcat seq (flatten packets))))
      (.write out packets))
    (.flush out)))

(defn client [handler & {:keys [host port]}]
  (let [running (atom true)]
    (future
      (with-open [socket (Socket. (or host HOST) (or port PORT))
                  _ (send-packets socket (packets/is-isi))]
        (while @running
          (let [in (receive-packets socket)
                out (handler in)]
            (send-packets socket out)))))
    running))
