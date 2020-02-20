(ns clj-insim.parsers
  (:require [clojure.set :as set]))

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

(def ^:private RST_RACE_FLAGS
  {1 :can-vote 2 :can-select 32 :mid-race 64 :must-pit
   128 :can-reset 256 :fcv 512 :cruise})

(def ^:private NCN_FLAGS
  {1 :female 2 :ai 4 :remote})

(def ^:private PLAYER_FLAGS
  {1 :swawpside 2 :reserved-2 4 :reserved-4 8 :autogears 16 :shifter
   32 :reserved-32 64 :help-b 128 :axis-clutch 256 :inputs 512 :autoclutch
   1024 :mouse 2048 :kb-no-help 4096 :kb-stabilized 8192 :custom-view})

(def ^:private SETF ;; NPL - Setup flags
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
  (let [maxx (->> (keys data-map) (apply max) (* 2))]
    (loop [data-map data-map, maxx maxx, x x]
      (if (<= 1 x (dec maxx))
        (find-flags data-map x)
        (recur
         data-map
         maxx
         (if (pos? x) (- x maxx) (+ x maxx)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parse functions

(defn- parse-race-laps [rl]
  (cond
    (#{0} rl) :practice
    (<= 100 rl 190) {:laps (-> rl (- 100) (* 10) (+ 100))}
    (<= 191 rl 238) {:hours (- rl 190)}
    :else {:laps rl}))

(defn- parse-tyre-compounds [[rear-l rear-r front-l front-r]]
  (let [compounds {0 :r1 1 :r2 2 :r3 3 :r4 4 :road-super
                   5 :road-normal 6 :hybrid 7 :knobbly 8 :num}]
    {:rear-l (compounds rear-l)
     :rear-r (compounds rear-r)
     :front-l (compounds front-l)
     :front-r (compounds front-r)}))

(defn- parse-leave-reason [n]
  (get
   {0 :none 1 :timeout 2 :lost-connection 3 :kicked 4 :banned
    5 :security 6 :cpw 7 :oos 8 :joos 9 :hack 10 :num}
   n))

(def body-key-parser
  {:sta/race-laps parse-race-laps
   :rst/race-laps parse-race-laps
   :sta/flags (partial flags ISS_STATE_FLAGS)
   :rst/race-flags (partial flags RST_RACE_FLAGS)
   :ncn/admin #(boolean (= 1 %))
   :ncn/flags (partial flags NCN_FLAGS)
   :npl/player-type (partial flags NCN_FLAGS)
   :npl/player-flags (partial flags PLAYER_FLAGS)
   :npl/tyres parse-tyre-compounds
   :npl/setup-flags (partial flags SETF)
   :cnl/reason parse-leave-reason})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Unparse functions

(defn- unparse-flags [data-map flags]
  (let [m (set/map-invert data-map)]
    (reduce + (map (partial get m) flags))))

(def body-key-unparser
  {:isi/flags (partial unparse-flags IS_FLAGS)})
