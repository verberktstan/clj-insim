(ns clj-insim.packets
  (:require [clj-insim.enums :as enums]
            [clj-insim.util :as util])
  (:import [java.nio ByteBuffer]))

(def ^:private INSIM-VERSION 7)

(def ^:private DEFAULTS {:admin util/null-char
                         :data 0
                         :flags (short 0)
                         :i-name "clj-insim"
                         :interval (short 0)
                         :prefix (int \space)
                         :reqi 0
                         :udp-port (short 0)})

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
  [byte-buffer x]
  (.put byte-buffer (.getBytes x)))

(defn- header
  "Returns a ByteBuffer with the InSim header"
  [{:keys [size type reqi data]}]
  (let [capacity (or size (:size DEFAULTS))
        byte-buffer (ByteBuffer/allocate capacity)]
    (doto byte-buffer
      (put-byte capacity)
      (put-byte (or type (enums/isp :tiny)))
      (put-byte (or reqi (:reqi DEFAULTS)))
      (put-byte (or data (:data DEFAULTS))))))

(defn- finalize
  "Returns a byte-array filled with bytes from byte-buffer"
  [byte-buffer]
  (let [size (.capacity byte-buffer)
        buffer (byte-array size)]
    (doto byte-buffer
      (.flip)
      (.get buffer))
    buffer))

(defn is-isi
  ([]
   (is-isi {}))
  ([{:keys [admin flags i-name interval prefix udp-port]}]
   (let [header (header {:size 44
                         :type (enums/isp :isi)
                         :reqi 1})
         packet (doto header
                  (put-word (or udp-port (:udp-port DEFAULTS)))
                  (put-word (or flags (:flags DEFAULTS)))
                  (put-byte INSIM-VERSION)
                  (put-byte (or prefix (:prefix DEFAULTS)))
                  (put-word (or interval (:interval DEFAULTS)))
;                  (.put (.getBytes (util/->cstring util/null-char 16)))
                  (put-string (util/->cstring (or admin (:admin DEFAULTS)) 16))
;                  (.put (.getBytes (util/->cstring "clj-insim" 16)))
                  (put-string (util/->cstring (or i-name (:i-name DEFAULTS)) 16))
                  )]
     (finalize packet))))

(defn is-tiny
  ([]
   (is-tiny {}))
  ([{:keys [data]}]
   (let [packet (header {:size 4
                         :type (enums/isp :tiny)
                         :data (or data (enums/tiny :none))})]
     (finalize packet))))

;;;; OLD STUFF BELOW!

(defn is-mst-packet
  [msg]
  (let [size 68 reqi 0 zero 0
        {:keys [byte-buffer buffer]} (allocate-buffers size)]
    (doto byte-buffer
      (.put (.byteValue size))
      (.put (.byteValue (enums/isp :mst))) ; Type ISP_MST
      (.put (.byteValue reqi))
      (.put (.byteValue zero))
      (.put (.getBytes (util/->cstring msg 64)))
      (.flip)
      (.get buffer))
    buffer))

(defn is-jrr-packet
  [player-id uniq-connection-id jrr-action]
  (let [size 68 reqi 0 zero 0
        {:keys [byte-buffer buffer]} (allocate-buffers size)]
    (doto byte-buffer
      (.put (.byteValue size))
      (.put (.byteValue (enums/isp :jrr)))
      (.put (.byteValue reqi))
      (.put (.byteValue player-id))
      (.put (.byteValue uniq-connection-id))
      (.put (.byteValue jrr-action))
      (.put (.byteValue 0)) ; spare
      (.put (.byteValue 0)) ; spare
      (.flip)
      (.get buffer))
    buffer))
