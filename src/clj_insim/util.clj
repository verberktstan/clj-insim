(ns clj-insim.util)

(def null-char "\u0000")

(defn- append-null
  "Returns string s with a null char appended"
  [s]
  (str s null-char))

(defn- pad-string
  "Return substring of s with null chars appended if shorter than length"
  [s length]
  (if (< (count s) length)
    (apply str (->> s (partition length length (repeat null-char)) first))
    (subs s 0 length)))

(defn ->cstring
  "Returns string s clipped to length and with a null char as last character"
  [s length]
  (->
   s
   (pad-string (dec length))
   append-null))

(defn strip-null-chars
  "Takes all values of vector v until it encounters a null char"
  [v]
  (take-while #(not= 0 %) v))

(def ->unsigned-byte
  (memoize
   (fn [x]
     (let [result (if (neg? x) (+ x 256) x)]
       (when (and (>= result -256) (< result 256)) result)))))

(defn join-request? [{:keys [type num-players]}]
  (and (= type :npl) (= num-players 0)))

(defn keep-alive-packet? [{:keys [type sub-type]}]
  (and (= type :tiny) (= sub-type :none)))
