(ns clj-insim.encoders
  (:require [clj-insim.enums :as enums]
            [org.clojars.smee.binary.core :as sb]))

(def encoders
  {:isi (sb/ordered-map
         :size :ubyte
         :type :ubyte
         :reqi :ubyte
         :zero :ubyte
         :udp-port :ushort-le
         :flags :ushort-le
         :insim-version :ubyte
         :prefix :ubyte
         :interval :ushort-le
         :admin (sb/padding (sb/c-string "UTF8") :length 16 :padding-byte 0)
         :iname (sb/padding (sb/c-string "UTF8") :length 16 :padding-byte 0))

   :msl (sb/ordered-map
         :size :ubyte
         :type (sb/enum :ubyte enums/ISP)
         :reqi :ubyte
         :sound :ubyte
         :message (sb/padding (sb/c-string "UTF8") :length 128 :padding-byte 0))

   :tiny (sb/ordered-map
          :size :ubyte
          :type (sb/enum :ubyte enums/ISP)
          :reqi :ubyte
          :sub-type (sb/enum :ubyte enums/TINY))})


