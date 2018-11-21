(ns clj-insim.core
  (:require [clj-sockets.core :as sockets]
            [clojure.java.io :as io]
            [clj-insim.packets :as packets]
            [clj-insim.types :as types]
            [clj-insim.util :as util])
  (:import [java.nio ByteBuffer]
           [java.net ServerSocket]
           [java.net Socket]))

(def HOST "127.0.0.1")
(def PORT 29999)

(def HEADER-SIZE 4)

(defn bytes->string [v]
  (->> v
       util/strip-null-chars
       (map char)
       (apply str)))

(defn parse-isp-ver-packet [v]
  (if (not= (count v) (- 20 HEADER-SIZE))
    (throw (Exception. "Packet is of invalid size"))
    (let [version (subvec v 0 8)
          product (subvec v 8 14)]
      {:version (bytes->string version)
       :product (bytes->string product)
       :insimver (int (nth v 14))
       :spare (nth v 15)})))

(defn parse-packet [v]
  (if (neg? (count v))
    (throw (Exception. "Packet size is not positive"))
    (let [[type reqi zero & body] v]
      (when (= type 2)
        (parse-isp-ver-packet (vec body))))))

(defn send-packet [{:keys [host port]} packet]
  (with-open [sock (Socket. (or host HOST) (or port PORT))
              out (io/output-stream sock)]
    (.write out packet)
    (.flush out)
    (let [in (io/input-stream sock)
          size (.read in)]
      (when (pos? size)
        (let [ba (byte-array (dec size))]
          (.read in ba)
          (vec ba))))))

(comment
  ;; (send socket (packets/is-isi-packet))
  ;; (send socket (packets/is-mst-packet "Hello from clj-insim!"))

  (send-packet {} (packets/is-isi-packet)) ; => Returns a packet received from LFS InSim
  (def received-packet (send-packet {} (packets/is-isi-packet)))
  (def received-packet [2 1 0 48 46 54 84 0 0 0 0 83 51 0 0 0 0 8 0])

  (count received-packet)

  (parse-packet received-packet)
)
