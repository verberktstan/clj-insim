(ns examples.scoring
  (:require [clj-insim.client :as client]
            [clj-insim.packets :as packets]
            [clojure.core.async :as a]))

(def POINTS (atom {}))

(defn- points [result-num]
  {:pre [(pos-int? result-num)]}
  (nth [25 18 15 12 10 8 6 4 2 1] (dec result-num) 0))

(defmulti dispatch (fn [_ {:header/keys [type]}] type))
(defmethod dispatch :default [_] nil)

(defmethod dispatch :res [client {:body/keys [confirmation-flags player-name result-num user-name] :as packet}]
  (newline)
  (println packet)
  (when (contains? confirmation-flags :confirmed)
    (swap! points update [user-name player-name] + (points result-num))))

(defn scoring []
  (let [{::client/keys [from-lfs-chan] :as client} (client/start)
        running? (atom true)
        stop! #(do (reset! running? false) (client/stop! client))]
    (a/go
      (while @running?
        (let [packet (a/<! from-lfs-chan)]
          (dispatch client packet))))
    {::stop stop!}))

(comment
  (def lfs-client (scoring))

  ((::stop lfs-client))

  @POINTS
)
