(ns clj-insim.parse
  (:require [clj-insim.enums :as enums]
            [clj-insim.parsers :as parsers]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn unparse [{:keys [type] :as header}]
  (let [enum (get enums/type-enum type)]
    (cond-> header
      enum (update :data #(get enum %))
      true (update :type #(get enums/ISP %)))))

(defn unparse-body [data]
  (reduce
   (fn [result [k v]]
     (if-let [parser (get parsers/body-key-unparser k)]
       (assoc result k (parser v))
       (assoc result k v)))
   {}
   (seq data)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn header [{:keys [type] :as header}]
  (let [enum (get enums/header-data type)]
    (cond-> header
      enum (update :data #(get enum %))
      true (update :type #(get enums/ISP-INV %)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parse body

(def ^:private dissoc-spares #(dissoc % :spare0 :spare1 :spare2 :spare3))

(defn- parse-body [data]
  (->
   (reduce
    (fn [result [k v]]
      (if-let [enum (get enums/body-key-enum k)]
        (assoc result k (get enum v))
        (if-let [parser (get parsers/body-key-parser k)]
          (assoc result k (parser v))
          (assoc result k v))))
    {}
    (seq data))
   dissoc-spares))

(defn body [data]
  (if (map? data)
    (parse-body data)
    data))
