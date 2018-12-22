(ns clj-insim.socket
  (:require [clj-insim.packets :as packets]
            [clojure.java.io :as io])
  (:import [java.net Socket]))

(def HOST "127.0.0.1")
(def PORT 29999)

(defn receive-packet [socket]
  (let [in (io/input-stream socket)
        size (.read in)
        ba (byte-array (dec size))]
    (.read in ba)
    (vec ba)))

(defn send-packet [socket packet]
  (let [out (io/output-stream socket)]
    (.write out packet)
    (.flush out)))

(defn make-socket [host port]
  (Socket. host port))
