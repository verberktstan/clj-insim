(ns clj-insim.flags)

(def BUTTON_STYLE [:c1 :c2 :c4 :click :light :dark :left :right])

(def CARS
  ["XFG" "XRG" "XRT" "RB4" "FXO" "LX4" "LX6" "MRT" "UF1" "RAC" "FZ5" "FOX" "XFR"
   "UFR" "FO8" "FXR" "XRR" "FZR" "BF1" "FBM"])

(def CONFIRMATION
  [:mentioned :confirmed :penalty-dt :penalty-sg :penalty-30 :penalty-45
   :did-not-pit])

(def ISI
  [:spare-0 :spare-1 :local :mso-cols :nlp :mci :con :obh :hlv :axm-load :axm-edit :req-join])

(def PIT_WORK
  [:nothing :stop:front-dam :front-wheels :left-front-damage :left-front-wheels
   :right-front-damage :right-front-wheels :right-damage :right-wheels
   :left-rear-damage :left-rear-wheels :right-rear-damage :right-rear-wheels
   :body-minor :body-major :setup :refuel])

(def PLAYER
  [:swapside :reserved-2 :reserved-4 :autogears :shifter :reserved-32 :help-b
   :axis-clutch :in-pits :autoclutch :mouse :kb-no-help :kb-stabilised
   :custom-view])

(def RST [:can-vote :can-select :mid-race :must-pit :can-reset :fcv :cruise])

(def SETUP [:symm-wheels :tc-enable :abs-enable])

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

