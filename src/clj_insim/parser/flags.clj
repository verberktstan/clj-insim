(ns clj-insim.parser.flags)

(defn- byte-checked?
  "Returns true if nth-byte is checked.
  (byte-checked? 9 8) => true
  (byte-checked? 9 1) => true
  (byte-checked? 9 2) => false
  (byte-checked? 9 4) => false"
  [x nth-byte]
  (>= (rem x (* 2 nth-byte)) nth-byte))

(defn- parse-byte-flags [protocol x]
  (reduce-kv (fn [m k v]
               (assoc m k (byte-checked? x v))) {} protocol))

;;;;; PUBLIC FUNCTIONS ;;;;;

(defn ->race
  "Flags (word) for the IS_RST packet"
  [x]
  (when (< x (* 2 512))
    (parse-byte-flags
     {:can-vote 1 :can-select 2 :mid-race 32 :must-pit 64 :can-reset 128 :fcv 256 :cruise 512} x)))

(defn ->state
  "Flags (word) for the IS_STA packet"
  [x]
  (when (< x ( * 2 32768))
    (parse-byte-flags
     {:game 1 :replay 2 :paused 4 :shiftu 8
      :dialog 16 :shiftu-follow 32 :shiftu-no-opt 64 :show-2d 128
      :front-end 256 :multi 512 :mpspeedup 1024 :windowed 2048
      :sound-mute 4096 :view-override 8192 :visible 16384 :text-entry 32768} x)))
