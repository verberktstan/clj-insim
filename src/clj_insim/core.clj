(ns clj-insim.core
  (:require [clj-sockets.core :as sockets]
            [clojure.java.io :as io]
            [clj-insim.packets :as packets]
            [clj-insim.types :as types]
            [clj-insim.util :as util]))

(defn receive
  "Read a byte of data from the input stream"
  [socket]
  (.read (io/input-stream socket)))

(defn send
  "Send the given packet (a byte-array) out over the given socket"
  [socket packet]
  (let [out (io/output-stream socket)]
    (.write out packet)
    (.flush out)))

(comment
  (def socket (sockets/create-socket "127.0.0.1" 29998))
  (send socket (packets/is-isi-packet))
  (send socket (packets/is-mst-packet "Hello from clj-insimthisistooomuchtoomuch for this simple network freakin connection!"))
)
