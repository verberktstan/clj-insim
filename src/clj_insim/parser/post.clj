(ns clj-insim.parser.post
  (:require [clojure.set :refer [rename-keys]]
            [clj-insim.enums :as enums]))

(defmulti parse :type)

(defmethod parse :flg [packet]
  (rename-keys packet {:data :player-id}))

(defmethod parse :tiny [{:keys [data] :as packet}]
  (-> packet
      (update :data enums/tiny-key)
      (rename-keys {:data :sub-type})))

(defmethod parse :default [packet] (identity packet))
