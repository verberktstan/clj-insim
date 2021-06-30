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

(defmethod dispatch :res [{:keys [to-lfs]}
                          {:body/keys [confirmation-flags player-name plate result-num]}]
  (when (contains? confirmation-flags :confirmed)
    (let [pnts (points result-num)
          mst-message (str plate " gains " pnts " points.")]
      ;; Inform about gained points
      (a/go (a/>! to-lfs (packets/mst {:message mst-message})))
      ;; Save points
      (if (contains? @POINTS player-name)
        (swap! POINTS update player-name + pnts)
        (swap! POINTS assoc player-name pnts))
      ;; Inform about new points amount
      (let [mst-message (str plate " now has " (get @POINTS player-name) " points.")]
        (a/go (a/>! to-lfs (packets/mst {:message mst-message})))))))

(defn scoring []
  (let [{:keys [from-lfs] :as client} (client/start)
        running? (atom true)
        stop #(do (reset! running? false) (client/stop client))]
    (reset! client/VERBOSE false)
    (a/go
      (while @running?
        (when-let [packet (a/<! from-lfs)]
          (dispatch client packet))))
    stop))

(comment
  (def scoring-client (scoring))

  (scoring-client)

  @POINTS
)
