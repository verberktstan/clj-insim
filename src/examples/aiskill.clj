(ns examples.aiskill
  (:gen-class)
  (:require [clj-insim.client :as client]
            [clj-insim.packets :as packets]
            [clojure.string]
            [clojure.string :as str]))

;; A simple example that ramps up an AI player's /aiset skill level every time
;; they cross the finish line (IS_LAP) or a split point (IS_SPX), based on a
;; difficulty preset. Use `/aiskill easy`, `/aiskill normal`, or `/aiskill hard`
;; to change the preset for all players (both existing and new).

(def ^:private skill-presets
  ;; /aiset accepts a level between 1 and 5
  {:easy   {:min-skill 2 :max-skill 5 :shuffle-odds-1-in 2}
   :normal {:min-skill 3 :max-skill 5 :shuffle-odds-1-in 3}
   :hard   {:min-skill 4 :max-skill 5 :shuffle-odds-1-in 4}})

(defonce ^:private players (atom {})) ;; player-id -> {:player-name :player-id :ucid :preset :current-skill}
(defonce ^:private current-difficulty (atom :hard)) ;; tracks the active difficulty preset

(defn- max-skill? [current-skill {:keys [max-skill]}]
  (>= current-skill max-skill))

(defn- shuffle? [{:keys [shuffle-odds-1-in]}]
  (-> shuffle-odds-1-in rand-int zero?))

(defn- random-skill-value [{:keys [min-skill max-skill]}]
  (-> max-skill (- min-skill) inc rand-int (+ min-skill)))

(defn- calculate-next-skill [current-skill preset-config]
  (cond
    (not (max-skill? current-skill preset-config)) (inc current-skill)
    (shuffle? preset-config)                       (random-skill-value preset-config)))

(defn- update-ai-skill! [client {:keys [player-id player-name preset current-skill]}]
  (let [preset-config (skill-presets preset)
        level         (calculate-next-skill current-skill preset-config)
        new-level?    (not= level current-skill)
        command       (packets/mst {:message (str "/aiset " player-name " " level)})]
    (when new-level?
      (swap! players assoc-in [player-id :current-skill] level)
      (client/>! client command))))

(defn- new-ai-player!
  "Starts tracking a newly joined AI player, assigning it a current
   difficulty preset and seeding its skill at that preset's `:max-skill`."
  [{:header/keys [player-id] :body/keys [player-name ucid]} preset]
  (swap! players assoc player-id
         {:player-name   player-name
          :player-id     player-id ;; duplicated so `update-ai-skill!` can `swap!` by id from the player map alone
          :ucid          ucid ;; tracked so a later IS_CPR rename (keyed by ucid, not player-id) can find this player
          :preset        preset
          :current-skill (get-in skill-presets [preset :max-skill])}))

(defn- rename-players-by-ucid! [ucid player-name]
  "IS_CPR is keyed by ucid, not player-id, so all tracked players owned by this
  connection need renaming"
  (swap! players
         (fn [players]
           (reduce-kv (fn [players player-id player]
                        (cond-> players
                          (= ucid (:ucid player))
                          (assoc-in [player-id :player-name] player-name)))
                      players
                      players))))

(defn- override-player-skills [players difficulty skill]
  (reduce-kv
   (fn override-player-skills* [players player-id _player]
     (cond-> players
       difficulty (assoc-in [player-id :preset] difficulty)
       skill      (assoc-in [player-id :current-skill] skill)))
   players
   players))

(defn- set-difficulty! [difficulty]
  (when (contains? skill-presets difficulty)
    (println "Setting difficulty to" difficulty)
    (reset! current-difficulty difficulty)
    (swap! players override-player-skills difficulty nil)
    (packets/mst {:message (str "ai skill preset set to " (name difficulty))})))

(def parse-difficulty (comp #{:hard :normal :easy} keyword))

(defn- parse-aiskill-command [s]
  (when (string? s)
    (when (str/starts-with? s "!ai")
      (update
        (->> (str/split s #" ")
             (map str/trim)
             (map str/lower-case)
             (remove str/blank?)
             (zipmap [:command :argument]))
        :argument parse-difficulty))))

(defn- handle-aiskill-command! [{:body/keys [text-start message user-type] :as packet}]
  (when (= :prefix user-type)
    (let [message                    (subs message text-start)
          {:keys [command argument]} (parse-aiskill-command message)]
      (println :message message :user-type user-type :command command :argument argument)
      (or
       (when (and command argument)
         (set-difficulty! argument))
       (when command
         (let [report-message (str "AI preset: "
                                   (name @current-difficulty)
                                   ", try: !ai hard, !ai normal or !ai easy")]
           (packets/mst {:message report-message})))))))

(defn- dispatch [client
   {:header/keys [type player-id ucid]
    :body/keys [player-type player-name new-ucid user-type message] :as packet}]
  (case type
    :npl        (when (= :ai player-type) (new-ai-player! packet @current-difficulty))
    :pll        (swap! players dissoc player-id)
    :plp        nil ;; player stays in the race (pit garage) - player-id is retained, nothing to update
    :cpr        (rename-players-by-ucid! ucid player-name)
    ;; car handed to a different connection - only relevant if we're already tracking it
    :toc        (when (contains? @players player-id)
                  (swap! players assoc-in [player-id :ucid] new-ucid))
    :mso        (when-let [response (handle-aiskill-command! packet)]
                  (client/>! client response))
    (:lap :spx) (when-let [player (get @players player-id)]
                  (update-ai-skill! client player))
    :ver        (client/>! client (packets/tiny {:request-info 1 :data :npl}))
    nil))

(defn aiskill
  "Starts the aiskill process; accepts opts with :host/:port. Returns stop fn."
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
       stop))))

(defn -main
  "Entrypoint for the uberjar. Usage: `java -jar aiskill.jar [host] [port]`."
  [& [host port]]
  (if (aiskill {:host host :port (some-> port Integer/parseInt)})
    @(promise) ;; block forever - the client runs on core.async's daemon threads
    (System/exit 1)))

(comment

  @players
  @current-difficulty

  ;; To start the aiskill process
  (def aiskill-client (aiskill))
  (def aiskill-client (aiskill {:host "192.168.2.11" :port 29999}))

  ;; To stop the client and aiskill process, simply call the stored function
  (aiskill-client)

  ;; Change difficulty and update all existing players (can also use /aiskill commands in-game)
  (set-difficulty! :easy)
  (set-difficulty! :normal)
  (set-difficulty! :hard))
