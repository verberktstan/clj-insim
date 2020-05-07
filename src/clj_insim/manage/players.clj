(ns clj-insim.manage.players
  (:require [clj-insim.models.packet :as packet]
            [clj-insim.packets :as packets]
            [clj-insim.queues :as queues]
            [clj-insim.register :as register :refer [register! unregister!]]))

(defn init! [players]
  (register/init! players))

(defmulti manage! (fn [{:keys [packet]}] (packet/type packet)))

(defmethod manage! :default [_]
  nil)

(defmethod manage! :npl [{:keys [packet out-queue players]}]
  (if (packet/reply? packet)
    (let [{:keys [player-id] :as player} (packet/npl->player packet)]
      (register! players player-id player))
    (queues/->queue out-queue (packets/tiny {:data :npl}))))

(defmethod manage! :pll [{:keys [packet out-queue players]}]
  (let [{:keys [player-id]} (packet/pll->player packet)]
      (unregister! players player-id)))

(defmethod manage! :tiny [{:keys [packet players]}]
  (when (packet/tiny-clear? packet)
    (init! players)))
