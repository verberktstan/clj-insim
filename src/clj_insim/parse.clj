(ns clj-insim.parse
  (:require [clj-insim.enums :as enums]
            [clj-insim.parsers :as parsers]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Post-parse to restructure data after it's been parsed

;; Parsed data is a (flat) map. In some cases you want to transform this data
;; into a more useable format. See examples below.

(defmulti post-parse :type)

(defmethod post-parse :default [_ _]
  nil)

;; E.g. with the IS_CON packet;
;; Instead of {:plid-a 1, :plid-b 2} => {:players [{:plid 1} {:plid 2}]}
(defmethod post-parse :con [header-data {:keys [plid-a steer-a plid-b steer-b] :as body-data}]
  {:players [{:plid plid-a, :steer steer-a}
             {:plid plid-b, :steer steer-b}]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Unparsing - Transform data from clj-insim spec to data thats ready to feed
;; into marshal codec. EG. IS_FLAG #{:req-join} => 2048

(defn unparse [{:keys [type] :as header}]
  (let [enum (get enums/type-enum type)]
    (cond-> header
      enum (update :data #(get enum %))
      true (update :type #(get enums/ISP %)))))

(defn unparse-body [data]
  (reduce
   (fn [result [k v]]
     (assoc result k (or (parsers/unparse k v) v)))
   {}
   (seq data)))

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
     (assoc
         result
       k
       (or (enums/parse k v) (parsers/parse k v) v)))
   {}
   (seq data)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The public parse/body function

(defn body [data header]
  (if (map? data)
    (let [parsed (parse-body data)]
      (or (post-parse header parsed) parsed))
    data))
