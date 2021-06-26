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

(def ^:private CAR_CONTACT
  (m/struct
   :car-contact/player-id m/ubyte
   :car-contact/info m/ubyte
   :car-contact/spare m/ubyte
   :car-contact/steer m/sbyte
   :car-contact/throttle-brake m/ubyte
   :car-contact/clutch-handbrake m/ubyte
   :car-contact/gear-spare m/ubyte
   :car-contact/speed m/ubyte
   :car-contact/direction m/ubyte
   :car-contact/heading m/ubyte
   :car-contact/acceleration-forward m/sbyte
   :car-contact/acceleration-right m/sbyte
   :car-contact/x m/sshort
   :car-contact/y m/sshort))

(def ^:private CAR_CONTACT_OBJECT
  (m/struct
   :car-contact/direction m/ubyte
   :car-contact/heading m/ubyte
   :car-contact/speed m/ubyte
   :car-contact/z-byte m/ubyte
   :car-contact/x m/ushort
   :car-contact/y m/ushort))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The values in the `body` map are functions that return a marshal struct.
;; Because in some cases we want to change the body codec based on the header
;; type/data. Take a look at `:small` for an example.

(def body
  {:axi
   (fn [_]
     (m/struct
      :body/autocross-start m/ubyte
      :body/num-checkpoints m/ubyte
      :body/num-objects m/ushort
      :body/layout-name (m/ascii-string 32)))

   :bfn
   (fn [_]
     (m/struct
      :body/ucid m/ubyte
      :body/click-id m/ubyte
      :body/click-max m/ubyte
      :body/inst m/ubyte))

   :btc
   (fn [_]
     (m/struct
      :body/click-id m/ubyte
      :body/inst m/ubyte
      :body/flags m/ubyte
      :body/spare m/ubyte))

   :btn
   (fn [{:header/keys [size]}]
     (m/struct
      :body/click-id m/ubyte
      :body/inst m/ubyte
      :body/button-style m/ubyte
      :body/type-in m/ubyte
      :body/left m/ubyte
      :body/top m/ubyte
      :body/width m/ubyte
      :body/height m/ubyte
      :body/text (m/ascii-string (- size 12))))

   :btt
   (fn [_]
     (m/struct
      :body/click-id m/ubyte
      :body/inst m/ubyte
      :body/type-in m/ubyte
      :body/spare m/ubyte
      :body/text (m/ascii-string 96)))

   :cch
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

   :con
   (fn [_]
     (m/struct
      :body/closing-speed m/ushort
      :body/time m/ushort
      :body/car-contacts
      (m/array CAR_CONTACT 2)))

   ;; Connection Player Renamed
   :cpr
   (fn [_]
     (m/struct
      :body/player-name (m/ascii-string 24)
      :body/plate (m/ascii-string 8)))

   ;; TODO: Add CPP Codec

   :fin
   (fn [_]
     (m/struct
      :body/total-time m/uint32 ;; unsigned
      :body/best-time m/uint32
      :body/spare-a m/ubyte
      :body/num-stops m/ubyte
      :body/confirm m/ubyte
      :body/spare-b m/ubyte
      :body/laps-done m/ushort
      :body/flags m/ushort))

   :flg
   (fn [_]
     (m/struct
      :body/off-on m/ubyte
      :body/flag m/ubyte
      :body/car-behind m/ubyte
      :body/spare m/ubyte))

   :hlv
   (fn [_]
     (m/struct
      :body/hlvc m/ubyte
      :body/spare m/ubyte
      :body/time m/ushort
      :body/car-contact CAR_CONTACT_OBJECT))

   :isi
   (fn [_]
     (m/struct
      :body/udp-port m/ushort
      :body/flags m/ushort
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
     :body/fuel-200 m/ubyte)

   ;; TODO: Add MCI packet

   :msl
   (fn [_]
     (m/struct
      :body/message (m/ascii-string 128)))

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

   :msx
   (fn [_]
     (m/struct
      :body/message (m/ascii-string 96)))

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

   :nlp
   (fn [{:header/keys [size]}]
     (let [body-size (- size 4)
           remainder (rem body-size 6)
           n (int (/ body-size 6))]
       (apply
        m/struct
        (concat
         [:body/nlp
          (m/array
           (m/struct
            :nlp/node m/ushort
            :nlp/lap m/ushort
            :nlp/player-id m/ubyte
            :nlp/position m/ubyte)
           n)]
         (when-not (zero? remainder)
           [:body/spare (m/ascii-string remainder)])))))

   :npl
   (fn [_]
     (m/struct
      :body/ucid m/ubyte
      :body/player-type m/ubyte
      :body/flags m/ushort

      :body/player-name (m/ascii-string 24)
      :body/plate (m/ascii-string 8)

      :body/car-name (m/ascii-string 4)
      :body/skin-name (m/ascii-string 16)
      :body/tyres (m/array m/ubyte 4)

      :body/handicap-mass m/ubyte
      :body/handicap-restriction m/ubyte
      :body/model m/ubyte
      :body/passenger m/ubyte

      :body/rear-wheel-adjustment m/ubyte
      :body/front-wheel-adjustment m/ubyte
      :body/spare (m/ascii-string 2)

      :body/setup-flags m/ubyte
      :body/num-player m/ubyte
      :body/config m/ubyte
      :body/fuel m/ubyte))

   :obh
   (fn [_]
     (m/struct
      :body/closing-speed m/ushort
      :body/time m/ushort
      :body/car-contact CAR_CONTACT_OBJECT
      :body/x m/sshort
      :body/y m/sshort
      :body/z-byte m/ubyte
      :body/spare m/ubyte
      :body/index m/ubyte
      :body/flags m/ubyte))

   :pen
   (fn [_]
     (m/struct
      :body/old-penalty m/ubyte
      :body/new-penalty m/ubyte
      :body/reason m/ubyte
      :body/spare m/ubyte))

   :pfl
   (fn [_]
     (m/struct
      :body/flags m/ushort
      :body/spare (m/ascii-string 2)))

   :pit
   (fn [_]
     (m/struct
      :body/laps-done m/ushort
      :body/flags m/ushort
      :body/fuel-added m/ubyte
      :body/penalty m/ubyte
      :body/num-stops m/ubyte
      :body/spare3 m/ubyte
      :body/tyres (m/array m/ubyte 4)
      :body/pit-work m/uint32
      :body/spare m/uint32))

   :pla
   (fn [_]
     (m/struct
      :body/fact m/ubyte
      :body/spare (m/ascii-string 3)))

   :psf
   (fn [_]
     (m/struct
      :body/stop-time m/uint32
      :body/spare m/uint32))

   :reo
   (fn [_]
     (m/struct
      :body/player-ids (m/array m/ubyte 40)))

   :res
   (fn [_]
     (m/struct
      :body/user-name (m/ascii-string 24)
      :body/player-name (m/ascii-string 24)
      :body/plate (m/ascii-string 8)
      :body/skin-name (m/ascii-string 4)
      :body/total-time m/uint32
      :body/best-time m/uint32
      :body/spare-a m/ubyte
      :body/num-stops m/ubyte
      :bdoy/confirmation-flags m/ubyte
      :body/spare-b m/ubyte
      :body/laps-done m/ushort
      :body/flags m/ushort
      :body/result-num m/ubyte
      :body/num-results m/ubyte
      :body/penalty-seconds m/ushort))

   ;; TODO add RIP codec

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
      :body/split3 m/ushort))

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
      :body/fuel-200 m/ubyte))

   ;; TODO add SSH codec

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
      :body/wind m/ubyte))

   :toc
   (fn [_]
     (m/struct
      :body/old-ucid m/ubyte
      :body/new-ucid m/ubyte
      :body/spare (m/ascii-string 2)))

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
      :body/spare (m/ascii-string 2)))})
