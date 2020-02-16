(ns clj-insim.parse
  (:require [clj-insim.enums :as enums]
            [clj-insim.parsers :as parsers]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn unparse [{:keys [type] :as header}]
  (let [enum (get enums/type-enum type)]
    (cond-> header
      enum (update :data #(get enum %))
      true (update :type #(get enums/ISP %)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn header [{:keys [type] :as header}]
  (let [enum (get enums/type-num->key-enum type)]
    (cond-> header
      enum (update :data #(get enum %))
      true (update :type #(get enums/ISP-INV %)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parse body

(defn- parse-body [data]
  (reduce
   (fn [result [k v]]
     (if-let [enum (get enums/body-key-enum k)]
       (assoc result k (get enum v))
       (if-let [parser (get parsers/body-key-parser k)]
         (assoc result k (parser v))
         (assoc result k v))))
   {}
   (seq data)))

(defn body [data]
  (if (map? data)
    (parse-body data)
    data))
