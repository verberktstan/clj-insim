(ns clj-insim.core
  (:require [clj-sockets.core :as sockets]
            [clojure.java.io :as io]
            [clj-insim.packets :as packets]
            [clj-insim.enums :as enums]
            [clj-insim.util :as util])
  (:import [java.nio ByteBuffer]
           [java.net Socket]))

(def HOST "127.0.0.1")
(def PORT 29999)

(def data (atom nil))

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

(defn parse-npl-packet [[type reqi player-id connection-id ptype & args]]
  (let [body (into [] args)
        flags (subvec body 0 2)
        player-name (subvec body 2 (+ 2 24))
        license-plate (subvec body 26 (+ 26 8))
        car-name (subvec body 34 (+ 34 4))
        skin-name (subvec body 38 (+ 38 16))
        tyres (subvec body 54 (+ 54 4))
        handicap-mass (nth body 58)
        handicap-restriction (nth body 59)]
    {:player-name player-name
     :handicap-mass handicap-mass
     :handicap-restriction handicap-restriction}))

(def type-dispatch
  {:ver (fn [[type reqi subt & body]]
          (println "Send mst package to LFS")
          (packets/is-mst-packet "Hello from clj-insim!"))
   :tiny (fn [[type reqi subt & body]]
           (let [tiny (enums/tiny-key (int subt))]
             (println "Tiny packet type: " (name tiny))
             (println "Send tiny/none packet to lfs")
             (packets/is-tiny-packet :none)))
   :mso (fn [[type reqi zero ucid plid user-type text-start & body]]
          (println (str "Received message of length " (count body)))
          (packets/is-tiny-packet :none))
   :npl (fn [packet]
          (reset! data packet)
          (packets/is-tiny-packet :none))})

;; Simple hander prints the type of packet received and returns a IS_TYNI/none packet to maintain connection
(defn simple-handler [packet]
  (let [[type reqi subt & body] packet
        type-key (enums/isp-key (int type))
        f (type-dispatch type-key)]
    (do
      (println (str "===== Received packet from LFS ====="))
      (println (str "Type: " type " / " (name type-key)))
      (if f ; Execute dispatch OR return keepalive packet
        (f packet)
        (packets/is-tiny-packet :none)))))

(comment
  ;; Start a tcp client with simple-handler
  (def simple-server (serve HOST PORT simple-handler))
  ;; To stop the client
  (reset! simple-server false)

  ;; example npl return:
  (def npl-packet [21 0 2 0 0 72 18 66 111 101 114 32 84 97 114 114 101 108 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 88 82 71 0 76 69 85 67 72 84 84 85 82 77 0 0 0 0 0 0 5 5 5 5 0 0 30 0 0 0 0 0 4 1 0 0])

  (parse-npl-packet npl-packet)
  )
 
