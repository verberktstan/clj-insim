(ns clj-insim.enlive
  (:require [clj-insim.models.packet :as packet]
            [clj-insim.packets :as packets]
            [clj-insim.queues :as queues]))

;; More realistic ai behavior

(defonce race-in-progress (atom :no-race))

(defmulti ai! (fn [{:keys [packet]}] (packet/type packet)))

(defmethod ai! :default [_] nil)

(defmethod ai! :sta [{:keys [packet]}]
  (reset! race-in-progress (-> packet ::packet/body :race-in-progress)))

(defn- autopit [out-queue ai-names]
  (let [names (atom (shuffle ai-names))]
    (Thread/sleep 2000) ;; Wait a bit to ensure race-in-progress is updated
    (while (and (#{:qualifying} @race-in-progress) (seq @names))
      (queues/->queue out-queue (packets/mst (str "/pitlane " (first @names))))
      (swap! names rest)
      (Thread/sleep (-> [3 5 8 8 13 21 34] shuffle first (* 1000))))))

(defn- autochange [out-queue ai-names]
  (let [names (atom (shuffle (take 32 (cycle ai-names))))]
    (Thread/sleep 60000)
    (while (and (#{:race} @race-in-progress) (seq @names))
      (queues/->queue out-queue (packets/mst (str "/aiset " (first @names) " " (-> [5 5 5 5 5 5 5 5 4 4 4 3] shuffle first))))
      (swap! names rest)
      (Thread/sleep (-> [8 13 21 21 34 55] shuffle first (* 1000))))))

(defmethod ai! :rst [{:keys [packet out-queue players]}]
  (let [{:keys [qual-mins race-laps]} (::packet/body packet)
        ai-names (->> @players
                      (filter (comp #{:ai} :player-type val))
                      vals
                      (map :player-name))]
    (cond
      ;; While qualifying, send random player to pitlane
      (and (pos? qual-mins) (#{:practice} race-laps))
      (future (autopit out-queue ai-names))

      ;; While racing, change AI skill randomly
      (and (-> race-laps :laps nil? not)
           (pos? (:laps race-laps)))
      (future (autochange out-queue ai-names)))))
