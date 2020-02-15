(ns clj-insim.parse
  (:require [clj-insim.enums :as enums]))

(defn- parser [m]
  (fn [n] (some #(when (= (val %) n) (key %)) m)))

(defn- unparser [m]
  (fn [k] (get m k)))

(def isp (parser enums/ISP))
(def unparse-isp (unparser enums/ISP))

(def unparse-tiny (unparser enums/TINY))

(def unparse-small (unparser enums/SMALL))

(def unparse-ttc (unparser enums/TTC))

(def user-type (parser enums/USER-TYPE))
(def unparse-user-type (unparser enums/USER-TYPE))

(defmulti header :type)

(defmethod header :default [header]
  header)

(defmethod header :tiny [header]
  (update header :data (parser enums/TINY)))

(defmethod header :small [header]
  (update header :data (parser enums/SMALL)))

(defmethod header :ttc [header]
  (update header :data (parser enums/TTC)))
