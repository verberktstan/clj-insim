(ns clj-insim.models.packet
  (:require [clojure.spec.alpha :as s]))

(s/def :header/size pos-int?)
(s/def :header/type (s/or :raw nat-int? :parsed keyword?))
(s/def :header/request-info nat-int?)
(s/def :header/data #(or (nat-int? %) (keyword? %)))

(s/def ::header
  (s/keys :req [:header/size :header/type :header/request-info]
          :opt [:header/data :header/player-id])) ;; data is somethings renamed to player-id 

(def ^:dynamic *strict-validation*
  "When true (default), parsed?/raw? validate the full header shape via
  clojure.spec. Defaults to false when the clj-insim.strict-validation
  system property is set to \"false\" (set by the built jar's launcher
  scripts). Bind or alter-var-root to override at runtime."
  (not= "false" (System/getProperty "clj-insim.strict-validation")))

(defn- conform-header-type [{:header/keys [type] :as packet}]
  (when (s/valid? ::header packet)
    (-> (s/conform :header/type type) first)))

(defn- valid-header? [{:header/keys [size type request-info]}]
  (and (pos-int? size)
       (or (nat-int? type) (keyword? type))
       (nat-int? request-info)))

(defn parsed? [packet]
  (if *strict-validation*
    (= :parsed (conform-header-type packet))
    (and (valid-header? packet) (keyword? (:header/type packet)))))

(defn raw? [packet]
  (if *strict-validation*
    (= :raw (conform-header-type packet))
    (and (valid-header? packet) (nat-int? (:header/type packet)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Specific packets

(defn maintain-connection?
  "Returns a truethy value when a TINY/NONE packet is passed in as argument."
  [{:header/keys [type data]}]
  (and (#{:tiny} type) (#{:none} data)))

