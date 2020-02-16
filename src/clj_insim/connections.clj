(ns clj-insim.connections
  (:require [clj-insim.models.packet :as packet]))

(defn ncn->connection [{::packet/keys [header body]}]
  #:connection{:id (:data header)
               :user-name (:user-name body)
               :player-name (:player-name body)
               :admin? (when (= (:admin body) 1) true)
               :flags (:flags body)})
