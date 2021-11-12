(ns examples.scoring
  (:require [clj-insim.client :as client]
            [clj-insim.packets :as packets]
            [examples.utils :as u]))

(defonce ^:private POINTS (atom {}))

(defn- points [result-num]
  {:pre [(nat-int? result-num)]}
  (nth [25 18 15 12 10 8 6 4 2 1] result-num 0))

(defmulti ^:private dispatch (fn [_ {:header/keys [type]}] type))
(defmethod dispatch :default [_ _] nil)

(defmethod dispatch :res [client
                          {:body/keys [confirmation-flags player-name plate result-num]}]
  (when (contains? confirmation-flags :confirmed)
    (let [pnts (points result-num)
          mst-message (str plate "(" player-name ") gains " pnts " points.")]
      ;; Inform about gained points
      (client/>!! client (packets/mst {:message mst-message}))
      ;; Save points
      (if (contains? @POINTS player-name)
        (swap! POINTS update player-name + pnts)
        (swap! POINTS assoc player-name pnts))
      ;; Inform about new points amount
      (let [mst-message (str plate "(" player-name ") now has " (get @POINTS player-name) " points.")]
        (client/>!! client (packets/mst {:message mst-message}))))))

(defn- scoring []
  (let [client (client/start)]
    (client/go client dispatch)
    #(client/stop client)))

(defn -main [& args]
  (u/main scoring))

(comment
  ;; To start the scoring client
  (def scoring-client (scoring))

  ;; To stop the scoring client
  (scoring-client)

  ;; Inspect the points
  (sort-by val > @POINTS)
)
