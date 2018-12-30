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

(defn ->confirmation
  "Flag (byte) for the IS_FIN packet"
  [x]
  (when (< x (* 2 64))
    (let [flags (parse-byte-flags
                 {:mentioned 1
                  :confirmed 2
                  :penalty-drive-trough 4
                  :penalty-stop-go 8
                  :penalty-30 16
                  :penalty-45 32
                  :did-not-pit 64} x)]
      (cond
        (reduce #(or %1 %2) (vals (select-keys flags [:penalty-drive-trough :penalty-stop-go :did-not-pit])))
        (assoc flags :disqualified true)

        (reduce #(or %1 %2) (vals (select-keys flags [:penalty-30 :penalty-45])))
        (assoc flags :time true)

        :else flags))))

(defn ->player [x]
  (when (< x (* 2 16384))
    (parse-byte-flags
     {:swapside 1
      :reserved-2 2
      :reserved-4 4
      :autogears 8
      :shifter 16
      :reserved-32 32
      :help-b 64
      :axis-clutch 128
      :inpits 256
      :autoclutch 512
      :mouse 1024
      :kb-no-help 2048
      :kb-stabilised 4096
      :custom-view 8192} x)))

(defn ->race
  "Flags (word) for the IS_RST packet"
  [x]
  (when (< x (* 2 512))
    (parse-byte-flags
     {:can-vote 1 :can-select 2 :mid-race 32 :must-pit 64 :can-reset 128 :fcv 256 :cruise 512} x)))

(defn ->setup [x]
  (when (< x (* 2 4))
    (parse-byte-flags
     {:symm-wheels 1 :tc-enable 2 :abs-enable 4} x)))

(defn ->state
  "Flags (word) for the IS_STA packet"
  [x]
  (when (< x ( * 2 32768))
    (parse-byte-flags
     {:game 1 :replay 2 :paused 4 :shiftu 8
      :dialog 16 :shiftu-follow 32 :shiftu-no-opt 64 :show-2d 128
      :front-end 256 :multi 512 :mpspeedup 1024 :windowed 2048
      :sound-mute 4096 :view-override 8192 :visible 16384 :text-entry 32768} x)))
