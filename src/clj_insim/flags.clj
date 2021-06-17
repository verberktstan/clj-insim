(ns clj-insim.flags)

(def CARS
  ["XFG" "XRG" "XRT" "RB4" "FXO" "LX4" "LX6" "MRT" "UF1" "RAC" "FZ5" "FOX" "XFR"
   "UFR" "FO8" "FXR" "XRR" "FZR" "BF1" "FBM"])

(def RST [:can-vote :can-select :mid-race :must-pit :can-reset :fcv :cruise])

(def STA
  [:game :replay :paused :shift-u :dialog :shift-u-follow :shift-u-no-opt
   :show-2d :front-end :multi :mspeedup :windowed :sound-mute :view-override
   :visible :text-entry])

(def SWITCHES [:set-signals :set-flash :headlights :horn :siren])

(defn- power-range
  "Returns a vector where each item in a range raised by the power of 2
  `(power-range 4) => [1 2 4 8]`"
  [n]
  {:pre [(pos-int? n)]}
  (into [] (comp (take n) (map #(int (Math/pow 2 %)))) (range)))

(defn parse
  "Returns a set of items from `coll` for a given integer `i`.
   This is useful to parse bit-flags.
   ```clojure
  (parse [:a :b] 2) => #{:b}`
  (parse [:a :b] 3) => #{:a :b}`
  ``` "
  [coll]
  {:pre [(sequential? coll)]}
  (fn [i]
    {:pre [(nat-int? i)]}
    (let [powr (power-range (count coll))
          get-flag #(nth coll (.indexOf powr %))]
      (loop [acc #{}, i i]
        (if (contains? (set powr) i)
          (conj acc (get-flag i))
          (let [below (apply max (take-while #(< % i) powr))]
            (recur (conj acc (get-flag below)) (- i below))))))))

(defn unparse
  "Returns integer representation of flags.
  ```clojure
  (unparse [:a :b :c] #{:b}) => 2
  (unparse [:a :b :c] #{:b}) => 2
  ```"
  [coll]
  {:pre [(sequential? coll)]}
  (fn [flags]
    {:pre [(set? flags)]}
    (int
     (reduce
      (fn [sum flag]
        (if (contains? (set coll) flag)
          (+ sum (Math/pow 2 (.indexOf coll flag)))
          sum))
      0
      flags))))

