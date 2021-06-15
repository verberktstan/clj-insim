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

   :ver
   (fn [_]
     (m/struct
      :body/version (m/ascii-string 8)
      :body/product (m/ascii-string 6)
      :body/insim-version m/ubyte
      :body/spare m/ubyte))

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
