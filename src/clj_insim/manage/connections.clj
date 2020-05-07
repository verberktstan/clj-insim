(ns clj-insim.manage.connections
  (:require [clj-insim.models.packet :as packet]
            [clj-insim.packets :as packets]
            [clj-insim.queues :as queues]
            [clj-insim.register :refer [register! unregister! init!]]))

(defmulti manage! (fn [{:keys [packet]}] (packet/type packet)))

(defmethod manage! :default [_]
  nil)

(defmethod manage! :ncn [{:keys [packet out-queue connections]}]
  (if (packet/reply? packet)
    (let [{:keys [connection-id] :as connection} (packet/ncn->connection packet)]
      (register! connections connection-id connection))
    (queues/->queue out-queue (packets/tiny {:data :ncn}))))

(defmethod manage! :cnl [{:keys [packet out-queue connections]}]
  (let [{:keys [connection-id]} (packet/cnl->connection packet)]
      (unregister! connections connection-id)))
