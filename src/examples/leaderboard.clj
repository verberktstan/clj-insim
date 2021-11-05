(ns examples.leaderboard
  (:require [clj-insim.client :as client]
            [clj-insim.packets :as packets]
            [clojure.core.async :as a]
            [examples.utils :as u]))

(def POSITIONS (atom #{}))

;; Returns a simple map with player-id and position for a given nodelap.
(defn parse-nodelap [{:nlp/keys [player-id position]}]
  (when (and player-id position)
    {:player-id player-id, :position position}))

;; Sends a button to LFS for each player/position entry.
(defn show-leaderboard! [client positions]
  (println "Sending leaderboard to lfs with BTN packets..")
  (doseq [{:keys [player-id position]} (sort-by :position positions)]
    (client/>!
     client
     (packets/btn {:text (str position ": player " player-id)
                   :request-info 1
                   :click-id player-id
                   :ucid 255
                   :button-style #{:left}
                   :left 0
                   :top (* 5 position)
                   :width 100
                   :height 5}))))

;; Returns true if the new positions differ from the old positions (set)
(defn diff?
  ([coll]
   (consume-nlps @POSITIONS coll))
  ([positions coll]
   (not= positions (set coll))))

(defmulti dispatch (fn [_ {:header/keys [type]}] type))
(defmethod dispatch :default [_ _] nil)

;; When a NLP packet arrives, parse the nlp collection. If there is a diff to the previous state => show the (new) leaderboard!
(defmethod dispatch :nlp [client {:body/keys [nlp]}]
  (let [parsed-nlps (map parse-nodelap nlp)]
    (if (diff? parsed-nlps)
      (let [new-positions (reset! POSITIONS (set parsed-nlps))]
        (show-leaderboard! client new-positions))
      (println "No changes in positions..."))))

;; Start leaderboard client, setting the NLP interval to 3000.
(defn leaderboard []
  (let [{:keys [from-lfs] :as client} (client/start {:isi (packets/isi {:flags #{:nlp}
                                                                        :interval 3000})})
        stop #(client/stop client)]
    (a/go
      (while (client/running? client)
        (when-let [packet (a/<! from-lfs)]
          (dispatch client packet))))
    stop))

(defn -main [& args]
  (u/main leaderboard))

(comment
  (def leaderboard-client (leaderboard))

  (leaderboard-client)
)
