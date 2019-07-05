(ns clj-insim.socket
  (:require [clojure.java.io :as io]
;            [clj-insim.packets :as packets]
;            [clj-insim.enums :as enums]
;            [clj-insim.util :as util]
;            [clj-insim.parser :refer [parse]]
            [org.clojars.smee.binary.core :as sb])
  (:import [java.net Socket]))

(def ISP
  {:none 0  :isi 1  :ver 2  :tiny 3 :small 4 :sta 5 :sch 6 :sfp 7 :scc 8 :cpp 9
    :ism 10 :mso 11 :iii 12 :mst 13 :mtc 14 :mod 15 :vtn 16 :rst 17 :ncn 18 :cnl 19
    :cpr 20 :npl 21 :plp 22 :pll 23 :lap 24 :spx 25 :pit 26 :psf 27 :pla 28 :cch 29
    :pen 30 :toc 31 :flg 32 :pfl 33 :fin 34 :res 35 :reo 36 :nlp 37 :mci 38 :msx 39
    :msl 40 :crs 41 :bfn 42 :axi 43 :axo 44 :btn 45 :btc 46 :btt 47 :rip 48 :ssh 49
    :con 50 :obh 51 :hlv 52 :plc 53 :axm 54 :acr 55 :hcp 56 :nci 57 :jrr 58 :uco 59
    :oco 60 :ttc 61 :slc 62 :csc 63 :cim 64})

(def TINY
  {:none 0 :ver 1 :close 2 :ping 3 :reply 4
   :vtc 5 :scp 6 :sst 7 :gth 8 :mpe 9
   :ism 10 :ren 11 :clr 12 :ncn 13 :npl 14
   :res 15 :nlp 16 :mci 17 :reo 18 :rst 19
   :axi 20 :axc 21 :rip 22 :nci 23 :alc 24
   :axm 25 :slc 26})

(def SMALL
  {:none 0 :ssp 1 :ssg 2 :vta 3 :tms 4
   :stp  5 :rtp 6 :nli 7 :alc 8 :lcs 9})

(def TTC
  {:none 0 :sel 1 :sel-start 2 :sel-stop 3})

(def isi-format
  (sb/ordered-map
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
   :iname (sb/padding (sb/c-string "UTF8") :length 16 :padding-byte 0)))

(def isi-packet
  {:size 44 :type (:isi ISP) :reqi 1 :zero 0
   :udp-port 0 :flags 0 :insim-version 8 :prefix (int (char \!))
   :interval 0 :admin "test" :iname "clj-insim"})

(def decoders
  {:tiny (sb/ordered-map
          :sub-type (sb/enum :ubyte TINY))

   :small (sb/ordered-map
           :sub-type (sb/enum :ubyte SMALL)
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
         :sub-type (sb/enum :ubyte TTC)
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

(defn client []
  (let [running (atom true)]
    (future
      (with-open [socket (Socket. "127.0.0.1" 29999)
                  input-stream (io/input-stream socket)
                  output-stream (io/output-stream socket)
                  _ (sb/encode isi-format output-stream isi-packet)
                  _ (.flush output-stream)]
        (while @running
          (if (pos? (.available input-stream))
            (let [header (sb/decode (sb/ordered-map :size :ubyte :type (sb/enum :ubyte ISP) :reqi :ubyte) input-stream)]
              (if-let [decoder (get decoders (:type header))]
                (prn (merge header (sb/decode decoder input-stream)))
                (prn (assoc header :raw (sb/decode (sb/repeated :ubyte :length (- (:size header) 3)) input-stream)))))
            (Thread/sleep 50)))))
    running))

(comment
  (def lfs (client))
  (reset! lfs false)
)
