(ns clj-insim.parser.post
  (:require [clojure.set :refer [rename-keys]]
            [clj-insim.enums :as enums]))

(defn- data->uniq-connection-id [p]
  (rename-keys p {:data :uniq-connection-id}))

(defmulti parse :type)

(defmethod parse :cnl [packet]
  (data->uniq-connection-id packet))

(defmethod parse :flg [packet]
  (rename-keys packet {:data :player-id}))

(defmethod parse :ncn [packet]
  (data->uniq-connection-id packet))

(defmethod parse :nci [packet]
  (data->uniq-connection-id packet))

(defmethod parse :npl [packet]
  (rename-keys packet {:data :player-id}))

(defmethod parse :slc [packet]
  (data->uniq-connection-id packet))

(defmethod parse :tiny [{:keys [data] :as packet}]
  (-> packet
      (update :data enums/tiny-key)
      (rename-keys {:data :sub-type})))

(defmethod parse :default [packet] (identity packet))
