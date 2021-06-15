(ns clj-insim.codecs
  (:require [marshal.core :as m]))

(def header
  (m/struct
   :header/size m/ubyte
   :header/type m/ubyte
   :header/request-info m/ubyte
   :header/data m/ubyte))

(def body
  {:isi
   (m/struct
    :body/udp-port m/ushort
    :body/is-flags m/ushort
    :body/insim-version m/ubyte
    :body/prefix m/ubyte
    :body/interval m/ushort
    :body/admin (m/ascii-string 16)
    :body/iname (m/ascii-string 16))

   :ver
   (m/struct
    :body/version (m/ascii-string 8)
    :body/product (m/ascii-string 6)
    :body/insim-version m/ubyte
    :body/spare m/ubyte)

   :small
   (m/struct
    :body/update-interval m/uint32)

   :sta
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
    :body/spare2 m/ubyte
    :body/spare3 m/ubyte
    :body/track (m/ascii-string 6)
    :body/weather m/ubyte
    :body/wind m/ubyte)})
