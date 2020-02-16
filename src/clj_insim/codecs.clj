(ns clj-insim.codecs
  (:require [marshal.core :as m]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Codecs

(def header
  (m/struct :size m/ubyte :type m/ubyte :request-info m/ubyte :data m/ubyte))

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

(defmethod body :mso [{:keys [size]}]
  (m/struct
   :connection-id m/ubyte
   :player-id m/ubyte
   :user-type m/ubyte
   :text-start m/ubyte
   :message (m/ascii-string (- size 4 4))))

(defmethod body :mst [{:keys [size]}]
  (m/struct
   :message (m/ascii-string (- size 4))))

(defmethod body :ncn [_]
  (m/struct
   :user-name (m/ascii-string 24)
   :player-name (m/ascii-string 24)
   :admin m/ubyte
   :total m/ubyte
   :flags m/ubyte
   :spare3 m/ubyte))

(defmethod body :ncl [_]
  (m/struct
   :reason m/ubyte
   :total m/ubyte
   :spare2 m/ubyte
   :spare3 m/ubyte))

(defmethod body :npl [_]
  (m/struct
   :connection-id m/ubyte
   :player-type m/ubyte
   :flags m/ushort ; insim word
   :player-name (m/ascii-string 24)
   :plate (m/ascii-string 8)
   :car-name (m/ascii-string 4)
   :skin-name (m/ascii-string 16)
   :tyres m/ubyte
   :handicap-mass m/ubyte
   :handicap-restriction m/ubyte
   :model m/ubyte
   :passengers m/ubyte
   :spare m/sint32 ; insim int
   :setup-flags m/ubyte
   :num-player m/ubyte
   :spare2 m/ubyte
   :spare3 m/ubyte))

(defmethod body :ver [_]
  (m/struct
   :version (m/ascii-string 8)
   :product (m/ascii-string 6)
   :insim-version m/ubyte
   :spare m/ubyte))
