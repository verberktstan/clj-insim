(ns clj-insim.codecs
  (:require [marshal.core :as m]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Codecs

(def header
  (m/array m/ubyte 4))

(defmulti body :type)

(defmethod body :default [{:keys [size]}]
  (m/array m/ubyte (- size 4)))

(defmethod body :isi [_]
  (m/struct
   :udp-port m/ushort
   :flags m/ushort
   :insim-version m/ubyte
   :prefix m/ubyte
   :interval m/ushort
   :admin (m/ascii-string 16)
   :iname (m/ascii-string 16)))

(defmethod body :mst [_]
  (m/struct
   :message (m/ascii-string 64)))

(defmethod body :ver [_]
  (m/struct
   :version (m/ascii-string 8)
   :product (m/ascii-string 6)
   :insim-version m/ubyte
   :spare m/ubyte))
