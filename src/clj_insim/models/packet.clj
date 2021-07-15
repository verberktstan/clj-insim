(ns clj-insim.models.packet
  (:require [clojure.spec.alpha :as s]))

(s/def :header/size pos-int?)
(s/def :header/type (s/or :raw nat-int? :parsed keyword?))
(s/def :header/request-info nat-int?)
(s/def :header/data #(or (nat-int? %) (keyword? %)))

(s/def ::header
  (s/keys :req [:header/size :header/type :header/request-info]
          :opt [:header/data :header/player-id])) ;; data is somethings renamed to player-id 

(defn- conform-header-type [{:header/keys [type] :as packet}]
  (when (s/valid? ::header packet)
    (-> (s/conform :header/type type) first)))

(defn parsed? [packet]
  (when-let [conformed (conform-header-type packet)]
    (= :parsed conformed)))

(defn raw? [packet]
  (when-let [conformed (conform-header-type packet)]
    (= :raw conformed)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Specific packets

(defn maintain-connection?
  "Returns a truethy value when a TINY/NONE packet is passed in as argument."
  [{:header/keys [type data]}]
  (and (#{:tiny} type) (#{:none} data)))

