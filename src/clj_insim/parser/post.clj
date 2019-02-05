(ns clj-insim.parser.post
  (:require [clojure.set :refer [rename-keys]]
            [clj-insim.enums :as enums]))

(defn- data->uniq-connection-id [p]
  (rename-keys p {:data :uniq-connection-id}))

(defn- data->player-id [p]
  (rename-keys p {:data :player-id}))

(defmulti parse :type)

(defmethod parse :cnl [packet]
  (data->uniq-connection-id packet))

(defmethod parse :flg [packet]
  (data->player-id packet))

(defmethod parse :lap [packet]
  (data->player-id packet))

(defmethod parse :ncn [packet]
  (data->uniq-connection-id packet))

(defmethod parse :nci [packet]
  (data->uniq-connection-id packet))

(defmethod parse :npl [packet]
  (data->player-id packet))

(defmethod parse :pll [packet]
  (data->player-id packet))

(defmethod parse :res [packet]
  (data->player-id packet))

(defmethod parse :reo [packet]
  (rename-keys packet {:date :num-players}))

(defmethod parse :slc [packet]
  (data->uniq-connection-id packet))

(defmethod parse :tiny [{:keys [data] :as packet}]
  (-> packet
      (update :data enums/tiny-key)
      (rename-keys {:data :sub-type})))

(defmethod parse :default [packet] (identity packet))
