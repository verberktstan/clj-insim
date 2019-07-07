(ns clj-insim.core
  (:require [clojure.java.io :as io]
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

(def encoders
  {:tiny (sb/ordered-map :size :ubyte :type (sb/enum :ubyte ISP) :reqi :ubyte :sub-type (sb/enum :ubyte TINY))
   :msl (sb/ordered-map :size :ubyte :type (sb/enum :ubyte ISP) :reqi :ubyte :sound :ubyte :message (sb/padding (sb/c-string "UTF8") :length 128 :padding-byte 0))})

(defn tiny [sub-type] {:size 4 :type :tiny :reqi 1 :sub-type sub-type})
(defn msl [message] {:size 132 :type :msl :reqi 1 :sound 0 :message message})

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

(defn enqueue! [queue packet]
  (swap! queue conj packet))

(defn pop! [queue]
  (when-let [packet (peek @queue)]
    (swap! queue pop)
    packet))

(defn client [running]
  (let [{:keys [in-queue out-queue] :as atoms}
        {:in-queue (atom (clojure.lang.PersistentQueue/EMPTY))
         :out-queue (atom (clojure.lang.PersistentQueue/EMPTY))}]
    (future
      (with-open [socket (Socket. "127.0.0.1" 29999)
                  input-stream (io/input-stream socket)
                  output-stream (io/output-stream socket)
                  _ (sb/encode isi-format output-stream isi-packet)
                  _ (.flush output-stream)]
        (while @running
          (cond
            (pos? (.available input-stream))
            (let [header (sb/decode (sb/ordered-map :size :ubyte :type (sb/enum :ubyte ISP) :reqi :ubyte) input-stream)
                  packet (if-let [decoder (get decoders (:type header))]
                           (merge header (sb/decode decoder input-stream))
                           (assoc header :raw (sb/decode (sb/repeated :ubyte :length (- (:size header) 3)) input-stream)))]
              (newline)
              (println "*** INCOMING PACKET ***")
              (enqueue! in-queue (doto packet println)))

            (peek @out-queue)
            (let [packet (pop! out-queue)]
              (when-let [encoder (get encoders (:type packet))]
                (newline)
                (println "### OUTGOING PACKET ###")
                (println packet)
                (sb/encode encoder output-stream packet)
                (.flush output-stream)))

            :else
            (Thread/sleep 20)))))
    atoms))

(defn handle [running in-queue handler out-queue]
  (future
    (while @running
      (if-let [packet (pop! in-queue)]
        (handler packet out-queue)
        (Thread/sleep 20)))))

(defmulti dispatch :type)
(defmethod dispatch :default [_ _] nil)

(defmethod dispatch :tiny [{:keys [sub-type]} queue]
  (when (= sub-type :none)
    (enqueue! queue (tiny :none))
    (enqueue! queue (msl "Maintained connection..."))))

(defmethod dispatch :ver [packet queue]
  (enqueue! queue (msl "Hello, World!")))

(defn start []
  (let [running (atom true)
        {:keys [in-queue out-queue] :as lfs-client} (client running)
        routine (handle running in-queue dispatch out-queue)]
    running))

(comment
  (def lfs-client (start))
  (reset! lfs-client false)
)


