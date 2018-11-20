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
    (apply str (->> s (partition length length (repeat null-char)) first))))

(defn ->cstring
  "Returns string s clipped to length and with a null char as last character"
  [s length]
  (->
   s
   (pad-string (dec length))
   append-null))
