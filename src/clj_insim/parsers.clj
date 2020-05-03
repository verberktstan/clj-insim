(ns clj-insim.parsers
  (:require [clj-insim.enums :as enums]
            [clojure.set :as set]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Data

(def ^:private ISS_STATE_FLAGS
  {1 :game 2 :replay 4 :paused 8 :shift-u 16 :dialog
   32 :shift-u-follow 64 :shift-u-no-opt 128 :show-2d 256 :front-end 512 :multi
   1024 :mspeedup 2048 :windowed 4096 :sound-mute 8192 :view-override 16834 :visible
   32768 :text-entry})

(def ^:private IS_FLAGS
  {1 :res0 2 :res1 4 :local 8 :mso-cols 16 :nlp
   32 :mci 64 :con 128 :obh 256 :hlv 512 :axm-load
   1024 :axm-edit 2048 :req-join})

(def ^:private PLAYER_FLAGS
  {1 :swapside 2 :reserved-2 4 :reserved-4 8 :autogears 16 :shifter
   32 :reserved-32 64 :help-b 128 :axis-clutch 256 :inputs 512 :autoclutch
   1024 :mouse 2048 :kb-no-help 4096 :kb-stabilized 8192 :custom-view})

(def ^:private PLC_CARS
  {1 "XFG" 2 "XRG" 4 "XRT" 8 "RB4" 16 "FXO"
   32 "LX4" 64 "LX6" 128 "MRT" 256 "UF1"
   512 "RAC" 1024 "FZ5" 2048 "FOX" 4096 "XFR"
   8192 "UFR" 16384 "FO8" 32766 "FXR" 65532 "XRR"
   131064 "FZR" 262128 "BF1" 524256 "FBM"})

(def ^:private PASSENGERS_FLAGS
  {1 :female 2 :front 4 :female 8 :rear-left 16 :female
   32 :rear-middle 64 :female 128 :rear-right})

(def ^:private SETUP_FLAGS
  {1 :symm-wheels 2 :tc-enable 4 :abs-enable})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helper functions

(defn- find-flag [data-map x]
  (loop [x x, y 0]
    (if-let [flag (get data-map x)]
      {:flag flag :rest y}
      (recur (dec x) (inc y)))))

(defn- find-flags [data-map x]
  (loop [x x, flags #{}]
    (if (#{0} x)
      flags
      (let [{:keys [flag rest]} (find-flag data-map x)]
        (recur rest (conj flags flag))))))

(defn- flags
  "Returns a set of flags deduced from data-map based on input x."
  [data-map x]
  (when (pos? x)
    (let [maxx (->> (keys data-map) (apply max) (* 2))]
      (loop [data-map data-map, maxx maxx, x x]
        (if (<= 1 x (dec maxx))
          (find-flags data-map x)
          (recur
           data-map
           maxx
           (- x maxx)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parse functions

(defn- parse-race-laps [rl]
  (cond
    (#{0} rl) :practice
    (<= 100 rl 190) {:laps (-> rl (- 100) (* 10) (+ 100))}
    (<= 191 rl 238) {:hours (- rl 190)}
    :else {:laps rl}))

(defn- parse-tyre-compounds [compounds]
  (let [compound {0 :r1 1 :r2 2 :r3 3 :r4 4 :road-super 5 :road-normal
                  6 :hybrid 7 :knobbly 8 :num}
        [rl rr fl fr] (map (partial get compound) compounds)]
    {:rear-left rl :rear-right rr
     :front-left fl :front-right fr}))

(def body-key-parser
  {:race-laps parse-race-laps
   :iss-state-flags (partial flags ISS_STATE_FLAGS)
   :player-flags (partial flags PLAYER_FLAGS)
   :passengers (partial flags PASSENGERS_FLAGS)
   :cars (partial flags PLC_CARS)
   :setup-flags (partial flags SETUP_FLAGS)
   :tyres parse-tyre-compounds})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Unparse functions

(defn- unparse-flags [data-map flags]
  (let [m (set/map-invert data-map)]
    (reduce + (map (partial get m) flags))))

(def body-key-unparser
  {:is-flags (partial unparse-flags IS_FLAGS)
   :cars (partial unparse-flags PLC_CARS)})
