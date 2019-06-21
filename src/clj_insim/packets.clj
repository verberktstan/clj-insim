(ns clj-insim.packets
  (:require [clj-insim.enums :as enums]
            [clj-insim.util :as util])
  (:import [java.nio ByteBuffer
            ByteOrder]))

(def ^:private INSIM-VERSION 8)

(defn- allocate-buffers [size]
  {:byte-buffer (ByteBuffer/allocate size)
   :buffer (byte-array size)})

(defn- put-byte
  "Return byte-buffer with x put as a byte"
  [byte-buffer x]
  (.put byte-buffer (.byteValue (int x))))

(defn- put-word
  "Return byte-buffer with x put as word (2-byte short)"
  [byte-buffer x]
  (.putShort byte-buffer (short x)))

(defn- put-unsigned
  "Return byte-buffer with x put as unsigned (4-byte int)"
  [byte-buffer x]
  (.putInt byte-buffer (int x)))

(defn- put-string
  "Return byte-buffer with x put as series of characters"
  [byte-buffer x n]
  (let [strng (util/->cstring x n)]
    (.put byte-buffer (.getBytes strng))))

(defn- header
  "Returns a ByteBuffer with the InSim header"
  [{:keys [size type reqi data]}]
  (let [capacity (or size 4)
        byte-buffer (doto (ByteBuffer/allocate capacity)
                      (.order (ByteOrder/LITTLE_ENDIAN)))]
    (doto byte-buffer
      (put-byte capacity)
      (put-byte (enums/isp (or type :tiny)))
      (put-byte (or reqi 1))
      (put-byte (or data 0)))))

(defn- finalize
  "Returns a byte-array filled with bytes from byte-buffer"
  [byte-buffer]
  (let [size (.capacity byte-buffer)
        buffer (byte-array size)]
    (doto byte-buffer
      (.flip)
      (.get buffer))
    buffer))

(defn isf
  "Returns an integer representing the IS_ISI bit flag options."
  [keys]
  (let [flags {:res-0 1 :res-1 2 :local 4 :mso-cols 8
               :nlp 16 :mci 32 :con 64 :obh 128
               :hlv 256 :axm-load 512 :axm-edit 1024 :req-join 2048}]
    (apply + (vals (select-keys flags keys)))))

(defn is-btn [{:keys [click-id inst-flags button-style type-in left top width height uniq-connection-id]} text]
  (let [t (apply str (take 240 text))
        text-size (* 4 (int (+ 0.75 (/ (+ (count t) 1) 4.0))))
        header (header {:size (+ 12 text-size) :type :btn :data uniq-connection-id})
        packet (doto header
                 (put-byte click-id)
                 (put-byte (or inst-flags 0))
                 (put-byte (or button-style 0))
                 (put-byte (or type-in 0))
                 (put-byte (or left 50))
                 (put-byte (or top 50))
                 (put-byte (or width 100))
                 (put-byte (or height 10))
                 (put-string t text-size))]
    (finalize packet)))

(defn is-hcp
  "Enforces restrictions for each car present as key in parameters where the value associated to the key should be a map like: {:mass 20 :restriction 10}"
  [{:keys [xfg xrg fbm xrt rb4 fxo lx4 lx6 mrt uf1 rac fz5 fox xfr ufr fo8 fxr xrr fzr bf1]}]
  (let [header (header {:size 68 :type :hcp})
        packet (doto header
                 (put-byte (or (:mass xfg) 0))
                 (put-byte (or (:restriction xfg) 0))
                 (put-byte (or (:mass xrg) 0))
                 (put-byte (or (:restriction xrg) 0))
                 (put-byte (or (:mass xrt) 0))
                 (put-byte (or (:restriction xrt) 0))
                 (put-byte (or (:mass rb4) 0))
                 (put-byte (or (:restriction rb4) 0))
                 (put-byte (or (:mass fxo) 0))
                 (put-byte (or (:restriction fxo) 0))
                 (put-byte (or (:mass lx4) 0))
                 (put-byte (or (:restriction lx4) 0))
                 (put-byte (or (:mass lx6) 0))
                 (put-byte (or (:restriction lx6) 0))
                 (put-byte (or (:mass mrt) 0))
                 (put-byte (or (:restriction mrt) 0))
                 (put-byte (or (:mass uf1) 0))
                 (put-byte (or (:restriction uf1) 0))
                 (put-byte (or (:mass rac) 0))
                 (put-byte (or (:restriction rac) 0))
                 (put-byte (or (:mass fz5) 0))
                 (put-byte (or (:restriction fz5) 0))
                 (put-byte (or (:mass fox) 0))
                 (put-byte (or (:restriction fox) 0))
                 (put-byte (or (:mass xfr) 0))
                 (put-byte (or (:restriction xfr) 0))
                 (put-byte (or (:mass ufr) 0))
                 (put-byte (or (:restriction ufr) 0))
                 (put-byte (or (:mass fo8) 0))
                 (put-byte (or (:restriction fo8) 0))
                 (put-byte (or (:mass fxr) 0))
                 (put-byte (or (:restriction fxr) 0))
                 (put-byte (or (:mass xrr) 0))
                 (put-byte (or (:restriction xrr) 0))
                 (put-byte (or (:mass fzr) 0))
                 (put-byte (or (:restriction fzr) 0))
                 (put-byte (or (:mass bf1) 0))
                 (put-byte (or (:restriction bf1) 0))
                 (put-byte (or (:mass fbm) 0))
                 (put-byte (or (:restriction fbm) 0))
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0)
                 (put-byte 0))]
    (finalize packet)))

(comment
  (is-hcp {:xrt {:restriction 2} :fxo {:restriction 5}}) ;; Send this packet to LFS and restrictions for XRT and FXO will be enforced
)

(defn is-isi
  ([]
   (is-isi {}))
  ([{:keys [admin flags i-name interval prefix udp-port]}]
   (let [header (header {:size 44
                         :type :isi
                         :reqi 1})
         packet (doto header
                  (put-word (or udp-port 0))
                  (put-word (or flags (isf [:req-join])))
                  (put-byte INSIM-VERSION)
                  (put-byte (or prefix (char \!)))
                  (put-word (or interval 0))
                  (put-string (or admin "abcde") 16)
                  (put-string (or i-name "clj-insim") 16))]
     (finalize packet))))

(defn is-tiny
  ([]
   (is-tiny {}))
  ([{:keys [data-key reqi]}]
   (let [packet (header {:size 4
                         :type :tiny
                         :reqi (or reqi 1)
                         :data (enums/tiny (or data-key :none))})]
     (finalize packet))))

(defn is-mst
  [msg]
  (let [header (header {:size 68
                        :type :mst})
        packet (doto header
                 (put-string msg 64))]
    (finalize packet)))

(defn is-msx
  [msg]
  (let [header (header {:size 100
                        :type :msx})
        packet (doto header
                 (put-string msg 96))]
    (finalize packet)))

(defn is-msl
  [msg]
  (let [header (header {:size 132
                        :type :msl})
        packet (doto header
                 (put-string msg 128))]
    (finalize packet)))

(defn is-mtc
  [uniq-connection-id player-id msg]
  (let [msg-size (+ (* 4 (int (/ (count msg) 4))) 4)
        msg-size (min msg-size 128)
        packet-size (+ 8 msg-size)
        header (header {:size packet-size
                        :type :mtc})
        packet (doto header
                 (put-byte uniq-connection-id)
                 (put-byte player-id)
                 (put-byte 0) ;spare
                 (put-byte 0)
                 (put-string msg msg-size))]
    (finalize packet)))

(defn is-plc
  "Returns a IS_PLC packet that restricts the allowed cars for a given connection"
  [uniq-connection-id cars]
  (let [m {:xfg 1 :xrg 2 :xrt 4 :rb4 8 :fxo 16 :lx4 32 :lx6 64 :mrt 128 :uf1 256 :rac 512 :fz5 1024
           :fox 2048 :xfr 4096 :ufr 8192 :fo8 16384 :fxr 32768 :xrr 65536 :fzr 131072 :bf1 262144 :fbm 524288}
        h (header {:size 12 :type :plc})
        packet (doto h
            (put-byte uniq-connection-id)
            (put-byte 0)
            (put-byte 0)
            (put-byte 0)
            (put-unsigned (apply + (vals (select-keys m cars)))))]
    (finalize packet)))

(defn is-reo [num-players new-order]
  (let [header (header {:size 44 :type :reo :data num-players})
        packet (doto header
                 (put-byte (if (> num-players 0) (nth new-order 0) 0))
                 (put-byte (if (> num-players 1) (nth new-order 1) 0))
                 (put-byte (if (> num-players 2) (nth new-order 2) 0))
                 (put-byte (if (> num-players 3) (nth new-order 3) 0))
                 (put-byte (if (> num-players 4) (nth new-order 4) 0))
                 (put-byte (if (> num-players 5) (nth new-order 5) 0))
                 (put-byte (if (> num-players 6) (nth new-order 6) 0))
                 (put-byte (if (> num-players 7) (nth new-order 7) 0))
                 (put-byte (if (> num-players 8) (nth new-order 8) 0))
                 (put-byte (if (> num-players 9) (nth new-order 9) 0))
                 (put-byte (if (> num-players 10) (nth new-order 10) 0))
                 (put-byte (if (> num-players 11) (nth new-order 11) 0))
                 (put-byte (if (> num-players 12) (nth new-order 12) 0))
                 (put-byte (if (> num-players 13) (nth new-order 13) 0))
                 (put-byte (if (> num-players 14) (nth new-order 14) 0))
                 (put-byte (if (> num-players 15) (nth new-order 15) 0))
                 (put-byte (if (> num-players 16) (nth new-order 17) 0))
                 (put-byte (if (> num-players 17) (nth new-order 17) 0))
                 (put-byte (if (> num-players 18) (nth new-order 18) 0))
                 (put-byte (if (> num-players 19) (nth new-order 19) 0))
                 (put-byte (if (> num-players 20) (nth new-order 20) 0))
                 (put-byte (if (> num-players 21) (nth new-order 21) 0))
                 (put-byte (if (> num-players 22) (nth new-order 22) 0))
                 (put-byte (if (> num-players 23) (nth new-order 23) 0))
                 (put-byte (if (> num-players 24) (nth new-order 24) 0))
                 (put-byte (if (> num-players 25) (nth new-order 25) 0))
                 (put-byte (if (> num-players 26) (nth new-order 26) 0))
                 (put-byte (if (> num-players 27) (nth new-order 27) 0))
                 (put-byte (if (> num-players 28) (nth new-order 28) 0))
                 (put-byte (if (> num-players 29) (nth new-order 29) 0))
                 (put-byte (if (> num-players 30) (nth new-order 30) 0))
                 (put-byte (if (> num-players 31) (nth new-order 31) 0))
                 (put-byte (if (> num-players 32) (nth new-order 32) 0))
                 (put-byte (if (> num-players 33) (nth new-order 33) 0))
                 (put-byte (if (> num-players 34) (nth new-order 34) 0))
                 (put-byte (if (> num-players 35) (nth new-order 35) 0))
                 (put-byte (if (> num-players 36) (nth new-order 36) 0))
                 (put-byte (if (> num-players 37) (nth new-order 37) 0))
                 (put-byte (if (> num-players 38) (nth new-order 38) 0))
                 (put-byte (if (> num-players 39) (nth new-order 39) 0)))]
    (finalize packet)))

(defn- put-object-info [byte-buffer {:keys [x y z-byte flags index heading]}]
  (doto byte-buffer
    (put-word (or x 0))
    (put-word (or y 0))
    (put-byte (or z-byte 0))
    (put-byte (or flags 0))
    (put-byte (or index 0))
    (put-byte (or heading 0))))

(defn is-jrr
  [{:keys [player-id uniq-connection-id jrr-action]}]
  (let [join-request? (or (= jrr-action (enums/jrr :spawn))
                          (= jrr-action (enums/jrr :reject)))
        data (if join-request? 0 player-id)
        ucid (if join-request? uniq-connection-id 0)
        header (header {:size 16
                        :type :jrr
                        :reqi 0
                        :data data})
        partial-packet (doto header
                         (put-byte ucid)
                         (put-byte (enums/jrr (or jrr-action :spawn)))
                         (put-byte 0) ; spare
                         (put-byte 0))
        packet (doto partial-packet
                 (put-object-info nil))]
    (finalize packet)))
