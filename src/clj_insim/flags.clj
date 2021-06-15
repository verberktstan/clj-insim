(ns clj-insim.flags)

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
  [coll i]
  {:pre [(sequential? coll) (nat-int? i)]}
  (let [powr (power-range (count coll))
        get-flag #(nth coll (.indexOf powr %))]
    (loop [acc #{}, i i]
      (if (contains? (set powr) i)
        (conj acc (get-flag i))
        (let [below (apply max (take-while #(< % i) powr))]
          (recur (conj acc (get-flag below)) (- i below)))))))

(defn unparse
  "Returns integer representation of flags.
  ```clojure
  (unparse [:a :b :c] #{:b}) => 2
  (unparse [:a :b :c] #{:b}) => 2
  ```"
  [coll flags]
  {:pre [(sequential? coll) (set? flags)]}
  (int
   (reduce
    (fn [sum flag]
      (if (contains? (set coll) flag)
        (+ sum (Math/pow 2 (.indexOf coll flag)))
        sum))
    0
    flags)))

