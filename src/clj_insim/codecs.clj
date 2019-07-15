(ns clj-insim.codecs
  (:require [clj-insim.enums :as enums]
            [org.clojars.smee.binary.core :as sb]))

(def ^:private decoders
  {:isi (sb/ordered-map
         :udp-port :ushort-le
         :flags :ushort-le
         :insim-version :ubyte
         :prefix :ubyte
         :interval :ushort-le
         :admin (sb/padding (sb/c-string "UTF8") :length 16 :padding-byte 0)
         :iname (sb/padding (sb/c-string "UTF8") :length 16 :padding-byte 0))

   :mso (fn [{:keys [size]}]
          (sb/ordered-map
           :ucid :ubyte
           :plid :ubyte
           :user-type :ubyte
           :text-start :ubyte
           :message (sb/padding (sb/c-string "UTF8") :length size :padding-byte 0))) ;; How to handle different sized packets?

   :msl (sb/ordered-map
         :message (sb/padding (sb/c-string "UTF8") :length 128 :padding-byte 0))

;   :tiny
;   (sb/ordered-map :sub-type (sb/enum :ubyte enums/TINY))

   :small (sb/ordered-map
;           :sub-type (sb/enum :ubyte enums/SMALL)
           :value :uint-le)

   :sta (sb/ordered-map
;         :zero :ubyte
         :replay-speed :float-le
         :flags :ushort-le
         :in-game-cam :ubyte
         :viewed-plid :ubyte
         :num-players :ubyte
         :num-connections :ubyte
         :num-finished :ubyte
         :race-in-progress (sb/enum :ubyte {:no-race 0 :race 1 :qualifying 2})
         :qualify-minutes :ubyte
         :race-laps :ubyte
         :spare2 :ubyte
         :spare3 :ubyte
         :track (sb/padding (sb/c-string "UTF8") :length 6 :padding-byte 0)
         :weather :ubyte
         :wind (sb/enum :ubyte {:off 0 :weak 1 :strong 2}))

   :ttc (sb/ordered-map
;         :sub-type (sb/enum :ubyte enums/TTC)
         :ucid :ubyte
         :b1 :ubyte
         :b2 :ubyte
         :b3 :ubyte)

   :ver (sb/ordered-map
;         :zero :ubyte
         :version (sb/padding (sb/c-string "UTF8") :length 8 :padding-byte 0)
         :product (sb/padding (sb/c-string "UTF8") :length 6 :padding-byte 0)
         :insim-version :ubyte
         :spare :ubyte)})

(defn header->body-codec [{:keys [type size] :as header}]
  (if (= type :mso)
    ((get decoders :mso) {:size (- size 8)})
    (get decoders type (sb/repeated :ubyte :length (- size 4)))))
