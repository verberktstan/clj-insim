(ns clj-insim.codecs
  (:require [marshal.core :as m]))

(def header
  (m/struct
   :header/size m/ubyte
   :header/type m/ubyte
   :header/request-info m/ubyte
   :header/data m/ubyte))

(def ^:private SMALL
  ;; NLI, SSG & SSP use the default `:body/interval` codec
  {:alc [:body/cars m/uint32]
   :lcs [:body/switches m/uint32]
   :rtp [:body/time m/uint32]
   :stp [:body/number m/uint32]
   :tms [:body/stop m/uint32]
   :vta [:body/action m/uint32]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The values in the `body` map are functions that return a marshal struct.
;; Because in some cases we want to change the body codec based on the header
;; type/data. Take a look at `:small` for an example.

(def body
  {:cch
   (fn [_]
     (m/struct
      :body/camera m/ubyte
      :body/spare (m/ascii-string 3)))

   ;; ConnectioN Leave
   :cnl
   (fn [_]
     (m/struct
      :body/reason m/ubyte
      :body/total m/ubyte
      :body/spare (m/ascii-string 2)))

   ;; Connection Player Renamed
   :cpr
   (fn [_]
     (m/struct
      :body/player-name (m/ascii-string 24)
      :body/plate (m/ascii-string 8)))

   ;; TODO: Add CPP Codec

   :isi
   (fn [_]
     (m/struct
      :body/udp-port m/ushort
      :body/is-flags m/ushort
      :body/insim-version m/ubyte
      :body/prefix m/ubyte
      :body/interval m/ushort
      :body/admin (m/ascii-string 16)
      :body/iname (m/ascii-string 16)))

   :iii
   (fn [{:header/keys [size]}]
     (m/struct
      :body/ucid m/ubyte
      :body/player-id m/ubyte
      :body/spare (m/ascii-string 2)
      ;; Take into account the header and body length
      :body/message (m/ascii-string (- size 4 4))))

   :ism
   (fn [_]
     (m/struct
      :body/host m/ubyte
      :body/spare (m/ascii-string 3)
      :body/host-name (m/ascii-string 32)))

   ;; TODO: Add MOD packet

   :lap
   (fn [_]
     :body/lap-time m/uint32
     :body/total-time m/uint32
     :body/laps-done m/ushort
     :body/flags m/ushort
     :body/spare0 m/ubyte
     :body/penalty m/ubyte
     :body/num-stops m/ubyte
     :body/spare3 m/ubyte)

   :mso
   (fn [{:header/keys [size]}]
     (m/struct
      :body/ucid m/ubyte
      :body/player-id m/ubyte
      :body/user-type m/ubyte
      :body/text-start m/ubyte
      ;; Take into account the header and body length
      :body/message (m/ascii-string (- size 4 4))))

   :mst
   (fn [_]
     (m/struct
      :body/message (m/ascii-string 64)))

   :mtc
   (fn [{:header/keys [size]}]
     (m/struct
      :body/ucid m/ubyte
      :body/player-id m/ubyte
      :body/spare (m/ascii-string 2)
      :body/text (m/ascii-string (- size 4 4))))

   :ncn
   (fn [_]
     (m/struct
      :body/user-name (m/ascii-string 24)
      :body/player-name (m/ascii-string 24)
      :body/admin m/ubyte
      :body/total m/ubyte
      :body/flags m/ubyte
      :body/spare m/ubyte))

   :npl
   (fn [_]
     (m/struct
      :body/ucid m/ubyte
      :body/player-type m/ubyte
      :body/flags m/ushort
      :body/car-name (m/ascii-string 4)
      :body/skin-name (m/ascii-string 16)
      :body/tyres (m/array m/ubyte 4)
      :body/handicap-mass m/ubyte
      :body/handicap-restriction m/ubyte
      :body/passenger m/ubyte
      :body/spare (m/ascii-string 4)
      :body/setup-flags m/ubyte
      :body/num-player m/ubyte
      :body/spare2 (m/ascii-string 2)))

   :ver
   (fn [_]
     (m/struct
      :body/version (m/ascii-string 8)
      :body/product (m/ascii-string 6)
      :body/insim-version m/ubyte
      :body/spare m/ubyte))

   :vtn
   (fn [_]
     (m/struct
      :body/ucid m/ubyte
      :body/action m/ubyte
      :body/spare (m/ascii-string 2)))

   :rst
   (fn [_]
     (m/struct
      :body/race-laps m/ubyte
      :body/qualify-minutes m/ubyte
      :body/num-players m/ubyte
      :body/timing m/ubyte
      :body/track (m/ascii-string 6)
      :body/weather m/ubyte
      :body/wind m/ubyte
      :body/flags m/ushort ; word
      :body/num-nodes m/ushort
      :body/finish m/ushort
      :body/split1 m/ushort
      :body/split2 m/ushort
      :body/split3 m/ushort
))

   :scc
   (fn [_]
     (m/struct
      :body/player-id m/ubyte
      :body/in-game-cam m/ubyte
      :body/spare (m/ascii-string 2)))

   :sch
   (fn [_]
     (m/struct
      :body/char m/ubyte
      :body/flag m/ubyte
      :body/spare (m/ascii-string 2)))

   :sfp
   (fn [_]
     (m/struct
      :body/flag m/ushort
      :body/off-on m/ubyte
      :body/spare m/ubyte))

   :small
   (fn [{:header/keys [data]}]
     (apply m/struct (get SMALL data [:body/interval m/uint32])))

   :spx
   (fn [_]
     (m/struct
      :body/split-time m/uint32 ;; unsigned
      :body/total-time m/uint32
      :body/split m/ubyte
      :body/penalty m/ubyte
      :body/num-stops m/ubyte
      :body/spare m/ubyte))

   :sta
   (fn [_]
     (m/struct
      :body/replay-speed m/float
      :body/flags m/ushort
      :body/in-game-cam m/ubyte
      :body/view-player-id m/ubyte
      :body/num-players m/ubyte
      :body/num-connections m/ubyte
      :body/num-finished m/ubyte
      :body/race-in-progress m/ubyte
      :body/qualify-minutes m/ubyte
      :body/race-laps m/ubyte
      :body/spare (m/ascii-string 2)
      :body/track (m/ascii-string 6)
      :body/weather m/ubyte
      :body/wind m/ubyte))})
