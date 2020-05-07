(ns clj-insim.codecs
  (:require [marshal.core :as m]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Codecs

(def header
  (m/struct :size m/ubyte :type m/ubyte :request-info m/ubyte :data m/ubyte))

(defmulti body :type)

(defmethod body :default [{:keys [size]}]
  (m/array m/ubyte (- size 4)))

(defmethod body :con [_]
  (m/struct
   :sp-close m/ushort
   :time m/ushort

   ;; CarContact A
   :plid-a m/ubyte
   :info-a m/ubyte
   :spare2-a m/ubyte
   :steer-a m/sbyte ;; insim char
   :throttle-brake-a m/ubyte
   :clutch-handbrake-a m/ubyte
   :gear-spare-a m/ubyte
   :speed-a m/ubyte
   :direction-a m/ubyte
   :heading-a m/ubyte
   :acceleration-f-a m/sbyte
   :acceleration-r-a m/sbyte
   :x-a m/sshort
   :y-a m/sshort

   ;; CarContact B
   :plid-b m/ubyte
   :info-b m/ubyte
   :spare2-b m/ubyte
   :steer-b m/sbyte ;; insim char
   :throttle-brake-b m/ubyte
   :clutch-handbrake-b m/ubyte
   :gear-spare-b m/ubyte
   :speed-b m/ubyte
   :direction-b m/ubyte
   :heading-b m/ubyte
   :acceleration-f-b m/sbyte
   :acceleration-r-b m/sbyte
   :x-b m/sshort
   :y-b m/sshort))

(defmethod body :isi [_]
  (m/struct
   :udp-port m/ushort
   :is-flags m/ushort
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

(defmethod body :mtc [{:keys [size]}]
  (m/struct
   :connection-id m/ubyte
   :player-id m/ubyte
   :spare2 m/ubyte
   :spare3 m/ubyte
   :message (m/ascii-string (- size 8))))

(defmethod body :ncn [_]
  (m/struct
   :user-name (m/ascii-string 24)
   :player-name (m/ascii-string 24)
   :admin m/ubyte
   :total m/ubyte
   :flags m/ubyte
   :spare3 m/ubyte))

(defmethod body :cnl [_]
  (m/struct
   :reason m/ubyte
   :total m/ubyte
   :spare2 m/ubyte
   :spare3 m/ubyte))

(defmethod body :npl [_]
  (m/struct
   :connection-id m/ubyte
   :player-type m/ubyte
   :player-flags m/ushort ; insim word
   :player-name (m/ascii-string 24)
   :plate (m/ascii-string 8)
   :car-name (m/ascii-string 4)
   :skin-name (m/ascii-string 16)
   :tyres (m/array m/ubyte 4)
   :handicap-mass m/ubyte
   :handicap-restriction m/ubyte
   :model m/ubyte
   :passengers m/ubyte
   :spare m/sint32 ; insim int
   :setup-flags m/ubyte
   :num-player m/ubyte
   :spare2 m/ubyte
   :spare3 m/ubyte))

(defmethod body :sta [_]
  (m/struct
   :replay-speed m/float
   :iss-state-flags m/ushort
   :in-game-cam m/ubyte
   :view-player-id m/ubyte
   :num-players m/ubyte
   :num-connections m/ubyte
   :num-finished m/ubyte
   :race-in-progress m/ubyte
   :qualify-minutes m/ubyte
   :race-laps m/ubyte
   :spare2 m/ubyte
   :spare3 m/ubyte
   :track (m/ascii-string 6)
   :weather m/ubyte
   :wind m/ubyte))

(defmethod body :ver [_]
  (m/struct
   :version (m/ascii-string 8)
   :product (m/ascii-string 6)
   :insim-version m/ubyte
   :spare m/ubyte))

(defmethod body :jrr [_]
  (m/struct
   :ucid m/ubyte
   :jrr-action m/ubyte
   :spare2 m/ubyte
   :spare3 m/ubyte

   ;; ObjectInfo
   :x m/sshort ; insim short
   :y m/sshort
   :z-byte m/ubyte
   :flags m/ubyte
   :index m/ubyte
   :heading m/ubyte))

(defmethod body :plc [_]
  (m/struct
   :ucid m/ubyte
   :spare1 m/ubyte
   :spare2 m/ubyte
   :spare3 m/ubyte
   :cars m/uint32 ; insim unsigned
   ))

(defmethod body :res [_]
  (m/struct
   :user-name (m/ascii-string 24)
   :player-name (m/ascii-string 24)
   :plate (m/ascii-string 8)
   :skin-prefix (m/ascii-string 4)
   :race-time m/uint32
   :best-lap-time m/uint32
   :spare-a m/ubyte
   :num-stops m/ubyte
   :confirmation-flags m/ubyte
   :spare-b m/ubyte
   :laps-done m/ushort ; insim word
   :flags m/ushort
   :result-num m/ubyte
   :num-results m/ubyte
   :penalty-seconds m/ushort))
