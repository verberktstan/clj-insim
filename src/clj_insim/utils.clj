(ns clj-insim.utils)

(defn c-str [s max-length]
  (-> (apply str s (repeat max-length (char 0)))
      (subs 0 (dec max-length))
      (str (char 0))))

;; TODO: Test/document this
(defn map-kv [coll fs]
  (reduce-kv
   (fn [m k f]
     (update m k f))
   coll
   fs))
