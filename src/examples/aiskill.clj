(ns examples.aiskill
  (:gen-class)
  (:require [clj-insim.client :as client]
            [clj-insim.packets :as packets]))

;; A simple example that ramps up an AI player's /aiset skill level every time
;; they cross the finish line (IS_LAP) or a split point (IS_SPX), based on a
;; randomly assigned difficulty preset.

(def ^:private skill-presets
  ;; /aiset accepts a level between 1 and 5
  {:easy   {:min-skill 1 :max-skill 4 :shuffle-odds-1-in 1}
   :normal {:min-skill 2 :max-skill 5 :shuffle-odds-1-in 2}
   :hard   {:min-skill 3 :max-skill 5 :shuffle-odds-1-in 3}})

(defonce ^:private players (atom {})) ;; player-id -> {:player-name :player-id :ucid :preset :current-skill}

(defn- max-skill? [current-skill {:keys [max-skill]}]
  (>= current-skill max-skill))

(defn- shuffle? [{:keys [shuffle-odds-1-in]}]
  (-> shuffle-odds-1-in rand-int zero?))

(defn- random-skill-value
  "Picks a random skill level within the preset's `:min-skill`/`:max-skill`
   range, inclusive on both ends."
  [{:keys [min-skill max-skill]}]
  (-> max-skill (- min-skill) inc rand-int (+ min-skill)))

(defn- update-ai-skill!
  "Advances `player`'s AI skill level by one, or reshuffles it to a random
   value within its preset once maxed out, then sends the corresponding
   `/aiset` command over `client` and records the new level in `players`."
  [client {:keys [player-id player-name preset current-skill]}]
  (let [preset-config (skill-presets preset)
        level         (cond
                        (not (max-skill? current-skill preset-config))
                        (inc current-skill)

                        (shuffle? preset-config)
                        (random-skill-value preset-config)

                        :else
                        current-skill)
        command       (packets/mst {:message (str "/aiset " player-name " " level)})]
    (swap! players assoc-in [player-id :current-skill] level)
    (client/>! client command)))

(defn- new-ai-player!
  "Starts tracking a newly joined AI player, assigning it a random
   difficulty preset and seeding its skill at that preset's `:max-skill`."
  [{:header/keys [player-id] :body/keys [player-name ucid]}]
  (let [preset (rand-nth [:easy :normal :normal :hard :hard :hard])]
    (swap! players assoc player-id
           {:player-name   player-name
            :player-id     player-id ;; duplicated so `update-ai-skill!` can `swap!` by id from the player map alone
            :ucid          ucid ;; tracked so a later IS_CPR rename (keyed by ucid, not player-id) can find this player
            :preset        preset
            :current-skill (get-in skill-presets [preset :max-skill])})))

(defn- rename-players-by-ucid!
  "IS_CPR (connection renamed) is keyed by ucid, not player-id, so every
   tracked player owned by that connection needs its name updated."
  [ucid player-name]
  (swap! players
         (fn [players]
           (reduce-kv (fn [players player-id player]
                        (cond-> players
                          (= ucid (:ucid player))
                          (assoc-in [player-id :player-name] player-name)))
                      players
                      players))))

(defn- dispatch
  "Routes an incoming InSim packet to the appropriate player-tracking or
   skill-update logic based on its `:header/type`."
  [client
   {:header/keys [type player-id ucid]
    :body/keys [player-type player-name new-ucid] :as packet}]
  (case type
    :npl        (when (= :ai player-type) (new-ai-player! packet))
    :pll        (swap! players dissoc player-id)
    :plp        nil ;; player stays in the race (pit garage) - player-id is retained, nothing to update
    :cpr        (rename-players-by-ucid! ucid player-name)
    ;; car handed to a different connection - only relevant if we're already tracking it
    :toc        (when (contains? @players player-id)
                  (swap! players assoc-in [player-id :ucid] new-ucid))
    (:lap :spx) (when-let [player (get @players player-id)]
                  (update-ai-skill! client player))
    nil))

(defn aiskill
  "Starts a process that ramps up an AI player's skill (per their assigned
   difficulty preset) every time they cross the finish line or a split point,
   occasionally reshuffling to a random skill within the preset's range.
   `opts` accepts `:host` and `:port` to override `client/start`'s defaults
   (127.0.0.1:29999)."
  ([] (aiskill nil))
  ([{:keys [host port]}]
   ;; `client/start` returns nil when it can't connect to LFS (e.g. `/insim`
   ;; hasn't been run yet), in which case there's nothing to start.
   (when-let [client (client/start (cond-> {} host (assoc :host host) port (assoc :port port)))]
     (let [stop #(client/stop client)]
       (client/go client dispatch)
       ;; Request an IS_NPL for every player already in the race - LFS only sends
       ;; them for players joining *after* we connect otherwise. ReqI must be
       ;; non-zero or LFS ignores the request.
       (client/>! client (packets/tiny {:request-info 1 :data :npl}))
       stop))))

(defn -main
  "Entrypoint for the uberjar. Usage: `java -jar aiskill.jar [host] [port]`."
  [& [host port]]
  (if (aiskill {:host host :port (some-> port Integer/parseInt)})
    @(promise) ;; block forever - the client runs on core.async's daemon threads
    (System/exit 1)))

(comment

  @players
  ;; To start the aiskill process
  (def aiskill-client (aiskill))
  (def aiskill-client (aiskill {:host "192.168.2.11" :port 29999}))

  ;; To stop the client and aiskill process, simply call the stored function
  (aiskill-client))
