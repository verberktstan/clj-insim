(ns clj-insim.core
  (:require [clj-sockets.core :refer [create-socket write-to close-socket listen create-server]])
  (:import [java.nio ByteBuffer]))

(defn ->cstring
  "Append a null char to the end of a string"
  [s]
  (str s "\u0000"))

(defn pad-string
  "Add null chars to end of string until string is of specified length"
  [s length]
  (if (< (count s) length)
    (apply str (first (partition length length (repeat "\u0000") s)))
    (subs s 0 length)))

(def IS_ISI {:size 44
             :type 1
             :reqi 0
             :zero 0
             :udp-port 0
             :flags 0
             :insim-ver 7
             :prefix \space
             :interval 0
             :password (-> "password" (pad-string 15) ->cstring)
             :iname (-> "mygame" (pad-string 15) ->cstring)})

(defn packet
  "Create a packet to send to LFS"
  [{:keys [size type reqi zero udp-port flags insim-ver prefix interval password iname]}]
  (let [byte-buffer (ByteBuffer/allocate size)
        buffer (byte-array size)]
    (doto byte-buffer
      (.put (.byteValue size)) ; byte (1 byte)
      (.put (.byteValue type))
      (.put (.byteValue reqi))
      (.put (.byteValue zero))
      (.putShort udp-port)    ; word (2 byte short)
      (.putShort flags)
      (.put (.byteValue insim-ver))
      (.put (.byteValue (int prefix)))
      (.putShort interval)
      (.put (.getBytes password))
      (.put (.getBytes iname))
      (.flip)
      (.get buffer))
    buffer))

(comment
  (into [] (packet IS_ISI))

  (def socket (create-socket "127.0.0.1" 29999))
  (write-to socket (String. (packet IS_ISI)))
  (write-to socket (String. (insim-packet {})))
)
