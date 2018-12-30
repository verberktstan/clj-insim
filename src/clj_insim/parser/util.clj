(ns clj-insim.parser.util
  (:require [clj-insim.util :refer [strip-null-chars]]))

;;;;; PARSER FUNCTIONS ;;;;;

(defn ->string [coll]
  (->> coll
       strip-null-chars
       (map char)
       (apply str)))

(defn ->unsigned [[a b c d]]
  (+ (bit-shift-left d 24) (bit-shift-left c 16) (bit-shift-left b 8) a))

(defn ->word [[a b]]
  (+ (bit-shift-left b 8) a))

;;;;; PARSER UTILITIES ;;;;;

(defn protocol-node
  "Create a node for the parser protocol with a byte length, key and parser"
  [k type]
  (let [prtcl (case type
                :int {:length 4 :parser ->unsigned}
                :unsigned {:length 4 :parser ->unsigned}
                :word {:length 2 :parser ->word}
                {:length 1})]
    (merge prtcl {:key k})))

(defn split
  "Split coll at 1 or at (:length k)"
  [coll k]
  (let [out (split-at (or (:length k) 1) coll)]
    (take-while #(seq %) out)))

(defn split-last [colls protocol]
  (concat (drop-last colls) (split (last colls) protocol)))
