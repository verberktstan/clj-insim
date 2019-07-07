(ns clj-insim.decoders
  (:require [clj-insim.enums :as enums]
            [org.clojars.smee.binary.core :as sb]))

(def decoders
  {:tiny (sb/ordered-map
          :sub-type (sb/enum :ubyte enums/TINY))

   :small (sb/ordered-map
           :sub-type (sb/enum :ubyte enums/SMALL)
           :value :uint-le)

   :sta (sb/ordered-map
         :zero :ubyte
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
         :track (sb/string "UTF8" :length 6)
         :weather :ubyte
         :wind (sb/enum :ubyte {:off 0 :weak 1 :strong 2}))

   :ttc (sb/ordered-map
         :sub-type (sb/enum :ubyte enums/TTC)
         :ucid :ubyte
         :b1 :ubyte
         :b2 :ubyte
         :b3 :ubyte)

   :ver (sb/ordered-map
         :zero :ubyte
         :version (sb/padding (sb/c-string "UTF8") :length 8 :padding-byte 0)
         :product (sb/padding (sb/c-string "UTF8") :length 6 :padding-byte 0)
         :insim-version :ubyte
         :spare :ubyte)})
