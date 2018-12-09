(ns clj-insim.cars)

(def ^:private POWER-WEIGHT {"XFG" {:power 115 :weight 942}
                   "XRG" {:power 140 :weight 1150}
                   "RB4" {:power 243 :weight 1210}
                   "FXO" {:power 234 :weight 1136}
                   "XRT" {:power 247 :weight 1223}
                   "LX4" {:power 140 :weight 499}
                   "LX6" {:power 190 :weight 539}
                   "RAC" {:power 245 :weight 800}
                   "FZ5" {:power 360 :weight 1380}})

(defn- positional-handicap-mass
  "Returns handicap mass for first 10 positions based on a power/weight ratio."
  [{:keys [power weight]}]
  (let [base (int (* (/ power weight) 180))]
    (map #(int (* base %)) [0.9 0.8 0.7 0.6 0.5 0.4 0.3 0.2 0.1 0.1])))

(defn- car-masses
  "Returns a collection of handicap masses for first 10 positions for a specific car."
  [car-name]
  (map #(assoc {} :mass %) (positional-handicap-mass (POWER-WEIGHT car-name))))

;; Map containing handicaps for first 10 positions for each car...
(def CAR-HANDICAPS
  (reduce #(assoc %1 %2 (car-masses %2)) {} (keys POWER-WEIGHT)))

(defn car-handicaps
  "Returns handicaps for position i for a specific car name"
  [car-name i]
  (-> CAR-HANDICAPS
      (get car-name)
      (nth i)))
