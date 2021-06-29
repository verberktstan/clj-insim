(ns examples.scoring
  (:require [clj-insim.client :as client]
            [clj-insim.packets :as packets]
            [clojure.core.async :as a]))

(defonce POINTS (atom {}))

(defn- points [result-num]
  {:pre [(nat-int? result-num)]}
  (nth [25 18 15 12 10 8 6 4 2 1] result-num 0))

(defmulti dispatch (fn [_ {:header/keys [type]}] type))
(defmethod dispatch :default [_ _] nil)

(defmethod dispatch :res [{::client/keys [to-lfs-chan]}
                          {:body/keys [confirmation-flags player-name plate result-num]}]
  (when (contains? confirmation-flags :confirmed)
    (let [pnts (points result-num)
          mst-message (str plate " gains " pnts " points.")]
      ;; Inform about gained points
      (a/go (a/>! to-lfs-chan (packets/mst {:message mst-message})))
      ;; Save points
      (if (contains? @POINTS player-name)
        (swap! POINTS update player-name + pnts)
        (swap! POINTS assoc player-name pnts))
      ;; Inform about new points amount
      (let [mst-message (str plate " now has " (get @POINTS player-name) " points.")]
        (a/go (a/>! to-lfs-chan (packets/mst {:message mst-message})))))))

(defn scoring []
  (let [{::client/keys [from-lfs-chan] :as client} (client/start)
        running? (atom true)
        stop! #(do (reset! running? false) (client/stop! client))]
    (a/go
      (while @running?
        (when-let [packet (a/<! from-lfs-chan)]
          (dispatch client packet))))
    {::stop stop!}))

(comment
  (def lfs-client (scoring))

  ((::stop lfs-client))

  @POINTS
  @client/ERROR_LOG
  (reset! client/ERROR_LOG nil)
  (reset! client/VERBOSE false)
)
