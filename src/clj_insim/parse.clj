(ns clj-insim.parse
  (:require [clj-insim.enums :as enums]
            [clj-insim.parsers :as parsers]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private data->enum
  {:tiny enums/TINY
   :small enums/SMALL
   :ttc enums/TTC})

(defn unparse [{:keys [type] :as header}]
  (let [enum (get data->enum type)]
    (cond-> header
      enum (update :data #(get enum %))
      true (update :type #(get enums/ISP %)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private data->inv-enum
  {(get enums/ISP :tiny) enums/TINY-INV
   (get enums/ISP :small) enums/SMALL-INV
   (get enums/ISP :ttc) enums/TTC-INV})

(defn header [{:keys [type] :as header}]
  (let [enum (get data->inv-enum type)]
    (cond-> header
      enum (update :data #(get enum %))
      true (update :type #(get enums/ISP-INV %)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private body-data->inv-enum
  {:in-game-cam enums/VIEW-IDENTIFIERS-INV
   :race-in-progress enums/RACE-IN-PROGRESS-INV
   :wind enums/WIND-INV})

(def ^:private body-data->parser
  {:race-laps parsers/parse-race-laps})

(defn- parse-body [data]
  (reduce
   (fn [result [k v]]
     (if-let [enum (get body-data->inv-enum k)]
       (assoc result k (get enum v))
       (if-let [parser (get body-data->parser k)]
         (assoc result k (parser v))
         (assoc result k v))))
   {}
   (seq data)))

(defn body [data]
  (if (map? data)
    (parse-body data)
    data))
