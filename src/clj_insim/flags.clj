(ns clj-insim.flags
  "Functionality to parse/unparse bit flags. Check `parse`, `unparse` and
   `clj-insim.flags-test` for more info.")

(def BUTTON_STYLE [:c1 :c2 :c4 :click :light :dark :left :right])

(def CARS
  ["XFG" "XRG" "XRT" "RB4" "FXO" "LX4" "LX6" "MRT" "UF1" "RAC" "FZ5" "FOX" "XFR"
   "UFR" "FO8" "FXR" "XRR" "FZR" "BF1" "FBM"])

(def CONFIRMATION
  [:mentioned :confirmed :penalty-dt :penalty-sg :penalty-30 :penalty-45
   :did-not-pit])

(def ISI
  [:spare-0 :spare-1 :local :mso-cols :nlp :mci :con :obh :hlv :axm-load :axm-edit :req-join])

(def NCN [:unknown-0 :unknown-1 :remote])

(def OBH [:layout :can-move :was-moving :on-spot])

(def PIT_WORK
  [:nothing :stop:front-dam :front-wheels :left-front-damage :left-front-wheels
   :right-front-damage :right-front-wheels :right-damage :right-wheels
   :left-rear-damage :left-rear-wheels :right-rear-damage :right-rear-wheels
   :body-minor :body-major :setup :refuel])

(def PLAYER
  [:swapside :reserved-2 :reserved-4 :autogears :shifter :reserved-32 :help-b
   :axis-clutch :in-pits :autoclutch :mouse :kb-no-help :kb-stabilised
   :custom-view])

(def PMO [:file-end :move-modify :selection-real :avoid-check])

(def RST [:can-vote :can-select :mid-race :must-pit :can-reset :fcv :cruise])

(def SETUP [:symm-wheels :tc-enable :abs-enable])

(def STA
  [:game :replay :paused :shift-u :dialog :shift-u-follow :shift-u-no-opt
   :show-2d :front-end :multi :mspeedup :windowed :sound-mute :view-override
   :visible :text-entry])

(def SWITCHES [:set-signals :set-flash :headlights :horn :siren])

(defn- flag-at-bit
  "Returns the item in `coll` at index `idx` when bit `idx` is set in `i`."
  [coll i idx]
  (when (bit-test i idx) (nth coll idx)))

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
    (into #{} (keep #(flag-at-bit coll i %)) (range (count coll)))))

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
    (reduce
     (fn [sum idx]
       (if (contains? flags (nth coll idx))
         (+ sum (bit-shift-left 1 idx))
         sum))
     0
     (range (count coll)))))

