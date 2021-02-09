(ns clj-insim.utils)

(defn equal-keys?
  "Returns true if all keys of map a are present in map b and associated with the same values."
  [a b]
  (= a (select-keys b (keys a))))

(defn strip-string [s]
  (apply str (take-while (partial not= (char 0)) s)))
