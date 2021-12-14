(ns examples.ai-skill
  (:require [clj-insim.client :as client]
            [clj-insim.packets :as packets]))

(defn grid-skills [n]
  (let [n-per-skill (cond-> (int (/ n 5)) (not (zero? (mod n 5))) inc)]
    (->> (range 5 0 -1)
         (mapcat (partial repeat n-per-skill))
         (take n))))

(defn gravitate [x target]
  (cond
    (> target x) (inc x)
    (< target x) (dec x)
    :else x))

(defmulti dispatch (fn [_ packet] (:header/type packet)))
(defmethod dispatch :default [_ _] nil)

(defonce PLAYERS (atom {}))

;; Todo - Request NPL on startup
(defmethod dispatch :npl [_ {:header/keys [player-id] :as packet}]
  (when-not (contains? @PLAYERS player-id)
    (swap! PLAYERS assoc player-id packet)))

(defmethod dispatch :pll [_ {:header/keys [player-id]}]
  (swap! PLAYERS dissoc player-id))

;; Todo - Store a randomized target skill for every ai on race start.
(defmethod dispatch :reo [client {:body/keys [player-ids]}]
  (let [pids (take-while (complement zero?) player-ids)
        skills (grid-skills (count pids))]
    (doseq [[player-id new-skill] (zipmap pids skills)]
      (let [player (get @PLAYERS player-id)]
        (when (contains? (:body/player-type player) :ai)
          (client/>!!
           client
           (packets/mst
            {:message (str "/aiset " (:body/player-name player) " " new-skill)})))))))

;; Todo - 'Gravitate' towards target skill on every split/lap

(defn ai-skill []
  (let [client (client/start)]
    (client/go client dispatch)
    #(client/stop client)))

(comment
  (def ai-skill-client (ai-skill))

  (ai-skill-client)

  @PLAYERS
  (group-by :body/player-type (vals @PLAYERS))
  (reset! PLAYERS {})

  (zipmap [23 11] [5 4])
)
