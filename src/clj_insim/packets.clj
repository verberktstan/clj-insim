(ns clj-insim.packets
  (:require [clj-insim.types :as types]
            [clj-insim.util :as util])
  (:import [java.nio ByteBuffer]))

(def ^:private INSIM-VERSION 7)

(def ^:private DEFAULTS {:reqi 0 :zero 0})

(defn- allocate-buffers [size]
  {:byte-buffer (ByteBuffer/allocate size)
   :buffer (byte-array size)})

(defn is-isi-packet
    "Create a InSim init packet to send to LFS"
    []
    (let [size 44
          udp-port 0
          flags 0
          interval 0
          {:keys [byte-buffer buffer]} (allocate-buffers size)]
      (doto byte-buffer
        (.put (.byteValue size)) ; byte (1 byte)
        (.put (.byteValue (types/isp :isi)))
        (.put (.byteValue (:reqi DEFAULTS))) ; If non-zero LFS will send IS_VER packet
        (.put (.byteValue (:zero DEFAULTS)))               ; 0
        (.putShort udp-port)    ; word (2 byte short) / Port for udp replies
        (.putShort flags) ; bit flags for options
        (.put (.byteValue INSIM-VERSION)) ; The INSIM_VERSION used
        (.put (.byteValue (int \space))) ; Special host message prefix
        (.putShort interval) ; Time (ms) between NLP or MCI
        (.put (.getBytes (util/->cstring util/null-char 16))) ; Admin password (if set)
        (.put (.getBytes (util/->cstring "clj-insim" 16))) ; A short name for this program
        (.flip)
        (.get buffer))
      buffer))

(comment
  (count (into [] (packets/is-isi-packet))) ; Should be 44
  (count (String. (packets/is-isi-packet))) ; Should be 44
)

(defn is-mst-packet
  [msg]
  (let [size 68 reqi 0 zero 0
        {:keys [byte-buffer buffer]} (allocate-buffers size)]
    (doto byte-buffer
      (.put (.byteValue size))
      (.put (.byteValue (types/isp :mst))) ; Type ISP_MST
      (.put (.byteValue (:reqi DEFAULTS)))
      (.put (.byteValue (:zero DEFAULTS)))
      (.put (.getBytes (util/->cstring msg 64)))
      (.flip)
      (.get buffer))
    buffer))
