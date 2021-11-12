(ns examples.safety
  (:require [clj-insim.client :as client]
            [clj-insim.packets :as packets]
            [examples.utils :as u]))

(defonce ^:private PLAYERS (atom {}))
(defonce ^:private SAFETY (atom {}))

(defn- get-player-name [player-id]
  (when-let [player (get @PLAYERS player-id)]
    (:body/player-name player)))

(defn- humanize [safety-rating]
  (let [round #(-> % (* 100) int (/ 100) float)]
    (min (round safety-rating) 100)))

(defn- key-by [f coll]
  (reduce (fn [m v] (assoc m (f v) v)) {} coll))

(defn- check-penalty! [send! player-name]
  (when-let [sr (get-in @SAFETY [player-name :safety/rating])]
    (cond
      (< sr 15) (send! (packets/mst {:message (str "/p_sg " player-name)}))
      (< sr 25) (send! (packets/mst {:message (str "/p_dt " player-name)})))))

(defn- register-safety
  "Register player (by name) in the safety map, if needed."
  [safety player-name]
  (cond-> safety
    ((complement contains?) safety player-name) (assoc player-name {:safety/rating 50})))

(defn- update-safety! [player-id f]
  (when-let [{:body/keys [player-name] :as player} (get @PLAYERS player-id)]
    (when (contains? @SAFETY player-name)
      (swap! SAFETY update-in [player-name :safety/rating] f))))

(def ^:private decrease-safety-rating (partial * 0.96125))
(def ^:private increase-safety-rating (partial * 1.0125))

(defn- report-new-safety-rating! [send! player-id]
  (when-let [{:body/keys [player-name] :as player} (get @PLAYERS player-id)]
    (when-let [sr (-> @SAFETY (get-in [player-name :safety/rating]) humanize)]
      (send!
       (packets/msl {:message (str player-name "'s new safety rating: " sr)
                     :sound :error})))))

(defn- send!
  "Returns a function that sends a packet to the client."
  [client]
  (partial client/>!! client))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dispatch based on packet type

(defmulti ^:private dispatch (fn [_ {:header/keys [type]}] type))
(defmethod dispatch :default [_ _] nil)

;; With every reported contact, (both) players' safety-rating is decreased
(defmethod dispatch :con [client {:body/keys [car-contacts] :as packet}]
  (let [player-ids (map :car-contact/player-id car-contacts)]
    (doall
     (for [player-id player-ids]
       (do
         (update-safety! player-id decrease-safety-rating)
         (report-new-safety-rating! (send! client) player-id)
         (check-penalty! (send! client) (get-player-name player-id)))))))

;; With every track limits warning, player's safety-rating is decreased
(defmethod dispatch :hlv [client {:header/keys [player-id] :as packet}]
  (update-safety! player-id decrease-safety-rating)
  (report-new-safety-rating! (send! client) player-id)
  (check-penalty! (send! client) (get-player-name player-id)))

;; After every lap, player's safety-rating is increased
(defmethod dispatch :lap [client {:header/keys [player-id] :as packet}]
  (update-safety! player-id increase-safety-rating)
  (report-new-safety-rating! (send! client) player-id))

;; Save all players in the PLAYERS atom
;; Register safety rating (if needed)
(defmethod dispatch :npl [client packet]
  (swap! PLAYERS assoc (:header/player-id packet) packet)
  (swap! SAFETY register-safety (:body/player-name packet)))

;; If player leaves, remove from the PLAYERS atom
(defmethod dispatch :pll [client packet]
  (swap! PLAYERS dissoc (:header/player-id packet)))

(defn- safety []
  (let [client (client/start)]
    (reset! PLAYERS {})
    (client/>!! client (packets/tiny {:request-info 1 :data :npl}))
    (client/go client dispatch)
    #(client/stop client)))

(defn -main [& args]
  (u/main safety))

(comment
  (def safety-client (safety))

  (safety-client)

  @PLAYERS
  (sort-by (comp :safety/rating val) @SAFETY)
)
