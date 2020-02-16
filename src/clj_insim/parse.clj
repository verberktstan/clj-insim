(ns clj-insim.parse
  (:require [clj-insim.enums :as enums]))

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
