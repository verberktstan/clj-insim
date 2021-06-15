(ns clj-insim.utils)

(defn c-str
  "Returns a string of length where the last char is always the null char. When
   string `s` is shorter than `length`, adds null chars to the end of the string."
  [s length]
  {:pre [(string? s) (pos-int? length)]}
  (-> (apply str s (repeat length (char 0)))
      (subs 0 (dec length))
      (str (char 0))))

(defn clip-str [s max div]
  (let [length (min (count s) (dec max))
        factor (loop [x (inc length)]
                 (if (zero? (mod x div)) x (recur (inc x))))]
    (c-str s factor)))

(defn map-kv
  "Returns coll with fns applied to coll. Works for vectors as well.
   `(map-kv {:a inc} {:a 1 :b 10})` => {:a 2 :b 10}
   `(map-kv {0 inc} [1 10])` => [2 10]"
  [fns coll]
  {:pre [(map? fns) (associative? coll)]}
  (reduce-kv (fn [m k f] (update m k f)) coll fns))

(defn index-of
  "Returns a function that returns the index of an item in sequential `coll`"
  [coll]
  {:pre [(sequential? coll)]}
  (fn [x]
    (let [idx (.indexOf coll x)]
      (when (nat-int? idx) idx))))
