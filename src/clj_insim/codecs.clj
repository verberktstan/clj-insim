(ns clj-insim.codecs
  (:require [marshal.core :as m]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Codecs

(def header
  (m/struct :size m/ubyte :type m/ubyte :request-info m/ubyte :data m/ubyte))

(defmulti body :type)

(defmethod body :default [{:keys [size]}]
  (m/array m/ubyte (- size 4)))

(defmethod body :cnl [_]
  (m/struct
   :cnl/reason m/ubyte
   :cnl/total m/ubyte
   :spare2 m/ubyte
   :spare3 m/ubyte))

(defmethod body :csc [_]
  (m/struct
   :spare0 m/ubyte
   :csc/action m/ubyte
   :spare2 m/ubyte
   :spare3 m/ubyte
   :csc/time m/uint32
   :csc/c (m/array m/ubyte 8)))

(defmethod body :flg [_]
  (m/struct
   :flg/off-on m/ubyte
   :flg/flag m/ubyte
   :flg/car-behind m/ubyte
   :spare3 m/ubyte))

(defmethod body :isi [_]
  (m/struct
   :isi/udp-port m/ushort
   :isi/flags m/ushort
   :isi/insim-version m/ubyte
   :isi/prefix m/ubyte
   :isi/interval m/ushort
   :isi/admin (m/ascii-string 16)
   :isi/iname (m/ascii-string 16)))

(defmethod body :mso [{:keys [size]}]
  (m/struct
   :mso/connection-id m/ubyte
   :mso/player-id m/ubyte
   :mso/user-type m/ubyte
   :mso/text-start m/ubyte
   :mso/message (m/ascii-string (- size 4 4))))

(defmethod body :mst [{:keys [size]}]
  (m/struct
   :mst/message (m/ascii-string (- size 4))))

(defmethod body :mtc [{:keys [size]}]
  (m/struct
   :mtc/connection-id m/ubyte
   :mtc/player-id m/ubyte
   :spare2 m/ubyte
   :spare3 m/ubyte
   :mtc/message (m/ascii-string (- size 8))))

(defmethod body :ncn [_]
  (m/struct
   :ncn/user-name (m/ascii-string 24)
   :ncn/player-name (m/ascii-string 24)
   :ncn/admin m/ubyte
   :ncn/total m/ubyte
   :ncn/flags m/ubyte
   :spare3 m/ubyte))

(defmethod body :npl [_]
  (m/struct
   :npl/connection-id m/ubyte
   :npl/player-type m/ubyte
   :npl/player-flags m/ushort ; insim word
   :npl/player-name (m/ascii-string 24)
   :npl/plate (m/ascii-string 8)
   :npl/car-name (m/ascii-string 4)
   :npl/skin-name (m/ascii-string 16)
   :npl/tyres (m/array m/ubyte 4)
   :npl/handicap-mass m/ubyte
   :npl/handicap-restriction m/ubyte
   :npl/model m/ubyte
   :npl/passengers m/ubyte
   :npl/spare m/sint32 ; insim int
   :npl/setup-flags m/ubyte
   :npl/num-players m/ubyte
   :spare2 m/ubyte
   :spare3 m/ubyte))

(defmethod body :reo [_]
  (m/struct
   :reo/player-ids (m/array m/ubyte 40)))

(defmethod body :rst [_]
  (m/struct
   :rst/race-laps m/ubyte
   :rst/qualify-minutes m/ubyte
   :rst/num-players m/ubyte
   :rst/timing m/ubyte ;;; TODO
   :rst/track (m/ascii-string 6)
   :rst/weather m/ubyte
   :rst/wind m/ubyte
   :rst/flags m/ushort ; insim word
   :rst/num-nodes m/ushort
   :rst/finish m/ushort
   :rst/split1 m/ushort
   :rst/split2 m/ushort
   :rst/split3 m/ushort))

(defmethod body :slc [_]
  (m/struct
   :slc/car-name (m/ascii-string 4)))

(defmethod body :sta [_]
  (m/struct
   :sta/replay-speed m/float
   :sta/flags m/ushort
   :sta/in-game-cam m/ubyte
   :sta/view-player-id m/ubyte
   :sta/num-players m/ubyte
   :sta/num-connections m/ubyte
   :sta/num-finished m/ubyte
   :sta/race-in-progress m/ubyte
   :sta/qualify-minutes m/ubyte
   :sta/race-laps m/ubyte
   :spare2 m/ubyte
   :spare3 m/ubyte
   :sta/track (m/ascii-string 6)
   :sta/weather m/ubyte
   :sta/wind m/ubyte))

(defmethod body :ver [_]
  (m/struct
   :ver/version (m/ascii-string 8)
   :ver/product (m/ascii-string 6)
   :ver/insim-version m/ubyte
   :spare m/ubyte))
