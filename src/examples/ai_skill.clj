(ns examples.ai-skill
  (:require [clj-insim.client :as client]
            [clj-insim.packets :as packets]
            [clojure.set :as set]))

;; Private data (state)
;; Refactor - Combine atoms above to one players map!
;; {1 {:skill 4 :target-skill 5 :player-name "AI 1" :player-type #{:ai}}}
(defonce ^:private PLAYERS (atom {}))

;; Private functions
(defn- grid-skills
  "Returns a list of skill levels for n racers on a grid.
  `(grid-skills 9) => [5 5 4 4 3 3 2 2 1]`"
  [n]
  (let [n-per-skill (cond-> (int (/ n 5)) (not (zero? (mod n 5))) inc)]
    (->> (range 5 0 -1)
         (mapcat (partial repeat n-per-skill))
         (take n))))

(defn- gravitate
  "Returns x gravitated to a target
   `(gravitate 3 5) => 4`"
  ([x] (gravitate x 5))
  ([x target]
   (cond
     (nil? x) 5
     (> target x) (inc x)
     (< target x) (dec x)
     :else x)))

(defn- ai-set!
  "Send a MST packet to the LFS client with the /aiset command for a given player
   and skill."
  [client player-name skill]
  (client/>!!
   client
   (packets/mst
    {:message (str "/aiset " player-name " " skill)})))

(defn- auto-gravitate-skill
  "Returns skill gravitated towards 5, n times.
   `(auto-gravitate-skill 1 2) => 3`"
  [skill n]
  (if (zero? n)
    skill
    (reduce gravitate skill (repeat n 5))))

(defn- random-skills [n]
  (repeatedly n #(-> 5 rand-int (auto-gravitate-skill (inc (rand-int 6))))))

(defn- auto-update-ai-skill
  ([client player-id]
   (auto-update-ai-skill client player-id 8))
  ([client player-id laps-done]
   (let [{:keys [player-name skill target-skill]} (get @PLAYERS player-id)]
     (cond
       (not (= skill target-skill))
       (when (< (rand) (/ 2 3))
         (let [new-skill (gravitate skill target-skill)]
           (swap! PLAYERS assoc-in [player-id :skill] new-skill)
           (ai-set! client player-name new-skill)))

       (< (rand) (/ 1 3))
       (let [new-target-skill (auto-gravitate-skill (inc (rand-int 5)) (rand-int laps-done))]
         (swap! PLAYERS assoc-in [player-id :target-skill] new-target-skill))))))

;; Dispatch methods

(defmulti dispatch (fn [_ packet] (:header/type packet)))
(defmethod dispatch :default [_ _] nil)

(defmethod dispatch :ver [client _]
  (reset! PLAYERS {})
  (client/>!! client (packets/tiny {:request-info 1 :data :npl})))

(defn trim-player-packet [packet]
  (-> packet
      (select-keys [:body/player-name :body/player-type])
      (set/rename-keys {:body/player-name :player-name :body/player-type :player-type})))

;; Todo - Request NPL on startup
(defmethod dispatch :npl [_ {:header/keys [player-id] :as packet}]
  (when-not (contains? @PLAYERS player-id)
    (swap! PLAYERS update player-id merge (trim-player-packet packet))))

(defmethod dispatch :pll [_ {:header/keys [player-id]}]
  (swap! PLAYERS dissoc player-id))

(defn start-skills [player-ids]
  (zipmap
   player-ids
   (map #(assoc {} :skill %1 :target-skill %2) (grid-skills (count player-ids)) (random-skills (count player-ids)))))

(defmethod dispatch :reo [client {:body/keys [player-ids]}]
  (let [pids (take-while (complement zero?) player-ids)
        psk (start-skills pids)]
    (doseq [[player-id {:keys [skill] :as sk}] psk]
      (let [{:keys [player-name player-type]} (get @PLAYERS player-id)]
        (when (contains? player-type :ai)
          (swap! PLAYERS update player-id merge sk)
          (ai-set! client player-name skill))))))

(defmethod dispatch :lap [client {:header/keys [player-id] :body/keys [laps-done]}]
  (swap! PLAYERS assoc-in [player-id :laps-done] laps-done)
  (auto-update-ai-skill client player-id laps-done))

(defmethod dispatch :spx [client {:header/keys [player-id]}]
  (let [{:keys [laps-done]} (get @PLAYERS player-id)]
    (auto-update-ai-skill client player-id (or (when (< (rand) 0.5) laps-done)
                                               8))))

(defn ai-skill []
  (let [client (client/start)]
    (client/go client dispatch)
    #(client/stop client)))

(comment
  (def ai-skill-client (ai-skill))

  (ai-skill-client)

  @PLAYERS
  (group-by :skill (vals @PLAYERS))
  (reset! PLAYERS {})
)

