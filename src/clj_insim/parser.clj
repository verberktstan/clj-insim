(ns clj-insim.parser
  (:require [clj-insim.enums :as enums]
            [clj-insim.util :refer [strip-null-chars]]))

(defn ->string [coll]
  (->> (strip-null-chars coll)
       (map char)
       (apply str)))

;; Parse protocols ;;
(defmulti protocol :type)

(defmethod protocol :default [_]
  nil)

(defmethod protocol :sta [_]
  [{:key :replay-speed :length 4 :parser ->string}
   {:key :flags :length 2 :parser ->string}
   :ingame-cam :viewed-player-id :num-players-in-race :num-connections :num-finished
   :race-in-progress :qualify-minutes :race-laps :spare-2 :spare-3
   {:key :track :length 6 :parser ->string}
   :weather :wind])

(defmethod protocol :ver [_]
  [{:key :version :length 8 :parser ->string}
   {:key :product :length 6 :parser ->string}
   :insim-version :spare])

(defn header
  "Parse the first 4 (header) bytes"
  [[size type reqi data]]
  {:size size :type (enums/isp-key type) :reqi reqi :data data})

(defn split
  "Split coll at 1 or at (:length k)"
  [coll k]
  (let [out (split-at (or (:length k) 1) coll)]
    (take-while #(seq %) out)))

(defn split-last [colls protocol]
  (concat (drop-last colls) (split (last colls) protocol)))

(defn protocol-key [protocol]
  (or (:key protocol) protocol))

(defn assoc-protocol [result protocol colls]
  (let [parser (or (-> protocol first :parser) first)]
    (if-not (seq colls)
      result
      (recur (assoc result (-> protocol first protocol-key) (parser (first colls))) (rest protocol) (rest colls)))))

(defn parse [packet]
  (when-let [header (header (take 4 packet))]
    (if-let [protocol (protocol header)]
      (let [colls (reduce split-last [(drop 4 packet)] protocol)
            body (assoc-protocol {} protocol colls)]
        (merge header body))
      header)))
;; (parse [20 2 1 0 48 46 54 84 0 0 0 0 83 51 0 0 0 0 8 0])
;; (parse (range 4))
