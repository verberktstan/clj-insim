(ns clj-insim.packets
  (:require [clj-insim.enums :as enums]
            [clj-insim.util :as util])
  (:import [java.nio ByteBuffer
            ByteOrder]))

(def ^:private INSIM-VERSION 8)

(defn- allocate-buffers [size]
  {:byte-buffer (ByteBuffer/allocate size)
   :buffer (byte-array size)})

(defn- put-byte
  "Return byte-buffer with x put as a byte"
  [byte-buffer x]
  (.put byte-buffer (.byteValue (int x))))

(defn- put-word
  "Return byte-buffer with x put as word (2-byte short)"
  [byte-buffer x]
  (.putShort byte-buffer (short x)))

(defn- put-string
  "Return byte-buffer with x put as series of characters"
  [byte-buffer x n]
  (let [strng (util/->cstring x n)]
    (.put byte-buffer (.getBytes strng))))

(defn- header
  "Returns a ByteBuffer with the InSim header"
  [{:keys [size type reqi data]}]
  (let [capacity (or size 4)
        byte-buffer (doto (ByteBuffer/allocate capacity)
                      (.order (ByteOrder/LITTLE_ENDIAN)))]
    (doto byte-buffer
      (put-byte capacity)
      (put-byte (or type (enums/isp :tiny)))
      (put-byte (or reqi 1))
      (put-byte (or data 0)))))

(defn- finalize
  "Returns a byte-array filled with bytes from byte-buffer"
  [byte-buffer]
  (let [size (.capacity byte-buffer)
        buffer (byte-array size)]
    (doto byte-buffer
      (.flip)
      (.get buffer))
    buffer))

(defn isf
  "Returns an integer representing the IS_ISI bit flag options."
  [keys]
  (let [flags {:res-0 1 :res-1 2 :local 4 :mso-cols 8
               :nlp 16 :mci 32 :con 64 :obh 128
               :hlv 256 :axm-load 512 :axm-edit 1024 :req-join 2048}]
    (apply + (vals (select-keys flags keys)))))

(defn is-isi
  ([]
   (is-isi {}))
  ([{:keys [admin flags i-name interval prefix udp-port]}]
   (let [header (header {:size 44
                         :type (enums/isp :isi)
                         :reqi 1})
         packet (doto header
                  (put-word (or udp-port 0))
                  (put-word (or flags (isf [:req-join])))
                  (put-byte INSIM-VERSION)
                  (put-byte (or prefix (char \!)))
                  (put-word (or interval 0))
                  (put-string (or admin "abcde") 16)
                  (put-string (or i-name "clj-insim") 16))]
     (finalize packet))))

(defn is-tiny
  ([]
   (is-tiny {}))
  ([{:keys [data-key reqi]}]
   (let [packet (header {:size 4
                         :type (enums/isp :tiny)
                         :reqi (or reqi 1)
                         :data (or (enums/tiny data-key) (enums/tiny :none))})]
     (finalize packet))))

(defn is-mst
  [msg]
  (let [header (header {:size 68
                        :type (enums/isp :mst)})
        packet (doto header
                 (put-string msg 64))]
    (finalize packet)))

(defn is-msx
  [msg]
  (let [header (header {:size 100
                        :type (enums/isp :msx)})
        packet (doto header
                 (put-string msg 96))]
    (finalize packet)))

(defn is-msl
  [msg]
  (let [header (header {:size 132
                        :type (enums/isp :msl)})
        packet (doto header
                 (put-string msg 128))]
    (finalize packet)))

(defn is-mtc
  [uniq-connection-id player-id msg]
  (let [msg-size (+ (* 4 (int (/ (count msg) 4))) 4)
        msg-size (min msg-size 128)
        packet-size (+ 8 msg-size)
        header (header {:size packet-size
                        :type (enums/isp :mtc)})
        packet (doto header
                 (put-byte uniq-connection-id)
                 (put-byte player-id)
                 (put-byte 0) ;spare
                 (put-byte 0)
                 (put-string msg msg-size))]
    (finalize packet)))

(defn is-reo [num-players new-order]
  (let [header (header {:size 4 :type (enums/isp :reo) :data num-players})
        packet (doto header
                 (put-string (str new-order) 40))]
    (finalize packet)))

(defn- put-object-info [byte-buffer {:keys [x y z-byte flags index heading]}]
  (doto byte-buffer
    (put-word (or x 0))
    (put-word (or y 0))
    (put-byte (or z-byte 0))
    (put-byte (or flags 0))
    (put-byte (or index 0))
    (put-byte (or heading 0))))

(defn is-jrr
  [{:keys [player-id uniq-connection-id jrr-action]}]
  (let [join-request? (or (= jrr-action (enums/jrr-action :spawn))
                          (= jrr-action (enums/jrr-action :reject)))
        data (if join-request? 0 player-id)
        ucid (if join-request? uniq-connection-id 0)
        header (header {:size 16
                        :type (enums/isp :jrr)
                        :reqi 1
                        :data data})
        partial-packet (doto header
                         (put-byte ucid)
                         (put-byte (or jrr-action (enums/jrr-action :spawn)))
                         (put-byte 0) ; spare
                         (put-byte 0))
        packet (doto partial-packet
                 (put-object-info nil))]
    (finalize packet)))
