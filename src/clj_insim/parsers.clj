(ns clj-insim.parsers)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Data

(def ^:private ISS_STATE_FLAGS
  {1 :game 2 :replay 4 :paused 8 :shift-u 16 :dialog
   32 :shift-u-follow 64 :shift-u-no-opt 128 :show-2d 256 :front-end 512 :multi
   1024 :mspeedup 2048 :windowed 4096 :sound-mute 8192 :view-override 16834 :visible
   32768 :text-entry})

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

(def body-key-parser
  {:race-laps parse-race-laps
   :iss-state-flags (partial flags ISS_STATE_FLAGS)})
