(ns clj-insim.enum
  (:require [clj-insim.utils :as u]))

(defn encode [enum]
  {:pre [(sequential? enum)]}
  (u/index-of enum))

(defn decode [enum]
  {:pre [(sequential? enum)]}
  (fn [idx]
    (when (contains? enum idx)
      (nth enum idx))))
