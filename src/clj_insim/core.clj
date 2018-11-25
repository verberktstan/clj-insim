(ns clj-insim.core
  (:require [clj-sockets.core :as sockets]
            [clojure.java.io :as io]
            [clj-insim.packets :as packets]
            [clj-insim.enums :as enums]
            [clj-insim.util :as util])
  (:import [java.nio ByteBuffer]
           [java.net Socket]
           ))

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

(defn serve [host port handler]
  (let [running (atom true)]
    (future
      (with-open [socket (Socket. host port)
                  _ (send-packet socket (packets/is-isi-packet))]
        (while @running
          (let [in (receive-packet socket)
                out (handler in)]
            (send-packet socket out)))))
    running))

;; Simple hander prints the type of packet received and returns a IS_TYNI/none packet to maintain connection
(defn simple-handler [[size type reqi subt & body]]
  (do
    (println (str "Received packet: " (name (enums/isp-key (int type)))))
    (packets/is-tiny-packet :none)))

(comment
  ;; Start a tcp client with simple-handler
  (def simple-server (serve "127.0.0.1" 29999 simple-handler))
  ;; To stop the client
  (reset! simple-server false)
)
