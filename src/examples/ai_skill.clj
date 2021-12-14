(ns examples.ai-skill
  (:require [clj-insim.client :as client]
            [clj-insim.packets :as packets]))

;; Private data (state)
(defonce ^:private PLAYERS (atom {})) ;; A map with player data keyed by player-id
(defonce ^:private TARGET_SKILLS (atom {})) ;; A map with target skills keyed by player-id
(defonce ^:private SKILLS (atom {})) ;; A map with current skills keyed by player-id

;; Private functions
(defn- grid-skills [n]
  (let [n-per-skill (cond-> (int (/ n 5)) (not (zero? (mod n 5))) inc)]
    (->> (range 5 0 -1)
         (mapcat (partial repeat n-per-skill))
         (take n))))

(defn- gravitate [x target]
  (cond
    (> target x) (inc x)
    (< target x) (dec x)
    :else x))

(defn- ai-set! [client player-name skill]
  (client/>!!
   client
   (packets/mst
    {:message (str "/aiset " player-name " " skill)})))

(defn- auto-gravitate-skill [skill n]
  (if (zero? n)
    skill
    (reduce gravitate skill (repeat n 5))))

(defn- random-skills [n]
  (repeatedly n #(-> 5 rand-int (auto-gravitate-skill (inc (rand-int 3))))))

(defn- auto-update-ai-skill [client player-id]
  (cond
    (not (= (get @SKILLS player-id) (get @TARGET_SKILLS player-id)))
    (let [new-skill (gravitate (get @SKILLS player-id) (get @TARGET_SKILLS player-id))]
      (swap! SKILLS assoc player-id new-skill)
      (ai-set! client (-> @PLAYERS (get player-id) :body/player-name) new-skill))

    (< (rand) 0.5)
    (let [new-target-skill (-> 5 random-skills shuffle first)]
      (swap! TARGET_SKILLS assoc player-id new-target-skill))))

;; Dispatch methods

(defmulti dispatch (fn [_ packet] (:header/type packet)))
(defmethod dispatch :default [_ _] nil)

(defmethod dispatch :ver [client _]
  (reset! PLAYERS {})
  (client/>!! client (packets/tiny {:request-info 1 :data :npl})))

;; Todo - Request NPL on startup
(defmethod dispatch :npl [_ {:header/keys [player-id] :as packet}]
  (when-not (contains? @PLAYERS player-id)
    (swap! PLAYERS assoc player-id (select-keys packet [:body/player-name :body/player-type]))))

(defmethod dispatch :pll [_ {:header/keys [player-id]}]
  (swap! PLAYERS dissoc player-id))

(defmethod dispatch :reo [client {:body/keys [player-ids]}]
  (let [pids (take-while (complement zero?) player-ids)
        skills (grid-skills (count pids))
        targets (random-skills (count pids))]
    (doseq [[player-id {:keys [skill target]}] (zipmap pids (map (fn [s t] {:skill s :target t}) skills targets))]
      (let [{:body/keys [player-name player-type]} (get @PLAYERS player-id)]
        (when (contains? player-type :ai)
          (swap! TARGET_SKILLS assoc player-id target)
          (swap! SKILLS assoc player-id skill)
          (ai-set! client player-name skill))))))

(defmethod dispatch :lap [client {:header/keys [player-id]}]
  (auto-update-ai-skill client player-id))

(defmethod dispatch :spx [client {:header/keys [player-id]}]
  (auto-update-ai-skill client player-id))

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
