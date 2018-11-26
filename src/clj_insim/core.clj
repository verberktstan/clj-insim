(ns clj-insim.core
  (:require [clj-sockets.core :as sockets]
            [clojure.java.io :as io]
            [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.protocols :as protocols]
            [clj-insim.util :as util])
  (:import [java.nio ByteBuffer]
           [java.net Socket]))

(def HOST "127.0.0.1")
(def PORT 29999)

;; (def data (atom nil))

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

(defn parse-bytes [{:keys [coll] :as m} {:keys [bytes cast key]}]
  (let [[c1 c2] (split-at bytes coll)]
    (if (empty? c2)
      (-> m ; When nothing left to parse..
          (dissoc :coll) ; Dissociate coll
          (assoc key (cast c1)))
      (-> m
          (assoc :coll c2) ; else replace coll
          (assoc key (cast c1))))))

;; (reduce parse-bytes {:coll is-ver-packet} (protocols :ver))

(defn parse-packet [packet protocol]
  (reduce parse-bytes {:coll packet} protocol))

(def type-dispatch
  {:ver (fn [packet]
          (let [m (parse-packet packet protocols/is-ver-protocol)]
            (println (str m))
            (packets/is-mst-packet "Hello from clj-insim!")))
   :tiny (fn [packet]
           (let [m (parse-packet packet protocols/is-tiny-protocol)]
             (println (str m))
             (packets/is-tiny-packet :none)))
   :mso (fn [packet]
          (let [m (parse-packet packet protocols/is-mso-protocol)]
            (println (str m))
            (packets/is-tiny-packet :none)))
   :npl (fn [packet]
          (let [m (parse-packet packet protocols/is-npl-protocol)]
            (println (str m))
            (packets/is-tiny-packet :none)))
   :sta (fn [packet]
          (let [m (parse-packet packet protocols/is-sta-protocol)]
            (println (str m))
            (packets/is-tiny-packet :none)))
   :flg (fn [packet]
          (let [m (parse-packet packet protocols/is-flg-protocol)]
            (println (str m))
            (packets/is-tiny-packet :none)))})

;; Simple hander prints the type of packet received and returns a IS_TYNI/none packet to maintain connection
(defn simple-handler [packet]
  (let [[type] packet
        type-key (enums/isp-key (int type))
        f (type-dispatch type-key)]
    (do
      (println (str "===== Received " (name type-key) " packet from LFS ====="))
      (if f ; Execute dispatch OR return keepalive packet
        (f packet)
        (packets/is-tiny-packet :none)))))

(comment
  ;; Start a tcp client with simple-handler
  (def simple-server (serve HOST PORT simple-handler))
  ;; To stop the client
  (reset! simple-server false)

  ;; example is-ver return:
  (def is-ver-packet [2 1 0 48 46 54 84 0 0 0 0 83 51 0 0 0 0 8 0])
  
  ;; example npl return:
  (def npl-packet [21 0 2 0 0 72 18 66 111 101 114 32 84 97 114 114 101 108 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 88 82 71 0 76 69 85 67 72 84 84 85 82 77 0 0 0 0 0 0 5 5 5 5 0 0 30 0 0 0 0 0 4 1 0 0])

  (parse-npl-packet npl-packet)

  (subvec (range 10) 3 4)
)
 
