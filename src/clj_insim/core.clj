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

(defn bytes->int [c] (-> c first int))
(defn bytes->string [c]
  (->>
   (map char (util/strip-null-chars c))
   (apply str)))
(defn bytes->isp-type [c]
  (-> c first enums/isp-key))
(defn bytes->tiny-subt [c]
  (-> c first enums/tiny-key))

(defn make-protocol [{:keys [key type length]}]
  (case type
    :string {:bytes length :cast bytes->string :key key}
    :type {:bytes 1 :cast bytes->isp-type :key key}
    :tiny-subt {:bytes 1 :cast bytes->tiny-subt :key key}
    {:bytes 1 :cast bytes->int :key key}))

(def is-ver-protocol [(make-protocol {:key :type :type :type})
                      (make-protocol {:key :reqi})
                      (make-protocol {:key :zero})
                      (make-protocol {:key :version :type :string :length 8})
                      (make-protocol {:key :product :type :string :length 6})
                      (make-protocol {:key :insim-version})
                      (make-protocol {:key :spare})])

(def is-tiny-protocol [(make-protocol {:key :type :type :type})
                       (make-protocol {:key :reqi})
                       (make-protocol {:key :subt :type :tiny-subt})])

(defn parse-bytes [{:keys [coll] :as m} {:keys [bytes cast key]}]
  (let [[c1 c2] (split-at bytes coll)]
    (if (empty? c2)
      (-> m ; When nothing left to parse..
          (dissoc :coll) ; Dissociate coll
          (assoc key (cast c1)))
      (-> m
          (assoc :coll c2) ; else replace coll
          (assoc key (cast c1))))))

;;(reduce parse-bytes {:coll is-ver-packet} (protocols :ver))

(def type-dispatch
  {:ver (fn [packet]
          (let [m (reduce parse-bytes {:coll packet} is-ver-protocol)]
            (println (str m))
            (packets/is-tiny-packet :none)))
   :tiny (fn [packet]
           (let [m (reduce parse-bytes {:coll packet} is-tiny-protocol)]
             (println (str m))
             (packets/is-tiny-packet :none)))
   :mso (fn [[type reqi zero ucid plid user-type text-start & body]]
          (println (str "Received message of length " (count body)))
          (packets/is-tiny-packet :none))
   :npl (fn [packet]
          (reset! data packet)
          (packets/is-tiny-packet :none))})

;; Simple hander prints the type of packet received and returns a IS_TYNI/none packet to maintain connection
(defn simple-handler [packet]
  (let [[type] packet
        type-key (enums/isp-key (int type))
        f (type-dispatch type-key)]
    (do
      (println (str "===== Received packet from LFS ====="))
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
 
