(ns clj-insim.parser.post
  (:require [clojure.set :refer [rename-keys]]
            [clj-insim.enums :as enums]))

(defn- data->uniq-connection-id [p]
  (rename-keys p {:data :uniq-connection-id}))

(defn- data->player-id [p]
  (rename-keys p {:data :player-id}))

(defn- data->sub-type [p]
  (rename-keys p {:data :sub-type}))

(defmulti parse :type)

(defmethod parse :cnl [packet]
  (data->uniq-connection-id packet))

(defmethod parse :flg [packet]
  (data->player-id packet))

(defmethod parse :fin [packet]
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
  (rename-keys packet {:data :num-players}))

(defmethod parse :slc [packet]
  (data->uniq-connection-id packet))

(defmethod parse :small [packet]
  (let [{:keys [sub-type] :as p}
        (-> packet
            (update :data enums/small)
            data->sub-type)]
    (case sub-type
      :vta
      (-> p
          (rename-keys {:value :action})
          (update :action enums/vote))

      p)))

(defmethod parse :spx [{:keys [data] :as packet}]
  (data->player-id packet))

(defmethod parse :tiny [{:keys [data] :as packet}]
  (-> packet
      (update :data enums/tiny)
      data->sub-type))

(defmethod parse :default [packet] (identity packet))
