(ns examples.aiskill
  (:gen-class)
  (:require [clj-insim.client :as client]
            [clj-insim.logging :as logging]
            [clj-insim.packets :as packets]
            [clojure.string :as str]))

;; A simple example that ramps up an AI player's /aiset skill level every time
;; they cross the finish line (IS_LAP) or a split point (IS_SPX), based on a
;; difficulty preset. Use `!ai easy`, `!ai normal`, or `!ai hard` to change the
;; preset for all players (both existing and new).

(def ^:private skill-presets
  ;; /aiset accepts a level between 1 and 5
  {:easy   {:min-skill 2 :max-skill 5 :shuffle-odds-1-in 2}
   :normal {:min-skill 3 :max-skill 5 :shuffle-odds-1-in 3}
   :hard   {:min-skill 4 :max-skill 5 :shuffle-odds-1-in 4}})

(defonce ^:private players (atom {})) ;; player-id -> {:player-name :player-id :ucid :preset :current-skill}
(defonce ^:private current-difficulty (atom :hard)) ;; tracks the active difficulty preset
(defonce ^:private volatility-override (atom {:shuffle-odds-1-in nil}))
(defonce ^:private game-state (atom {:race-in-progress? false})) ;; tracks if in race/quali session

(defn- max-skill? [current-skill {:keys [max-skill]}]
  (>= current-skill max-skill))

(defn- shuffle? [{override-n :shuffle-odds-1-in} {preset-n :shuffle-odds-1-in}]
  (some-> (or override-n preset-n 3) rand-int zero?))

(defn- random-skill-value [{:keys [min-skill max-skill]}]
  (-> max-skill (- min-skill) inc rand-int (+ min-skill)))

(defn- calculate-next-skill [current-skill volatility-override preset-config]
  (cond
    (not (max-skill? current-skill preset-config))    (inc current-skill)
    (shuffle? volatility-override preset-config) (random-skill-value preset-config)))

;; Let's store volatility override with the user
(defn- update-ai-skill! [{:keys [player-id player-name preset current-skill]}]
  (let [preset-config (skill-presets preset)
        level         (calculate-next-skill current-skill @volatility-override preset-config)
        new-level?    (not= level current-skill)]
    (when (and level new-level?)
      (swap! players assoc-in [player-id :current-skill] level)
      (packets/mst {:message (str "/aiset " player-name " " level)}))))

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

(def ^:private volatility-mapping {:rare 4 :balanced 3 :frequent 2})

(defn- set-difficulty! [difficulty volatility]
  (when (contains? skill-presets difficulty)
    (println "Setting difficulty to" (name difficulty))
    (println "Setting volatility to" (or (some-> volatility name) "default"))
    (reset! current-difficulty difficulty)
    (swap! players override-player-skills difficulty nil)
    (reset! volatility-override {:shuffle-odds-1-in (get volatility-mapping volatility)}) ;; Could be nil or a keyword
    [(packets/mst {:message (str "ai difficulty set to " (name difficulty))})
     (packets/mst {:message (str "ai volatility set to " (or (some-> volatility name) "default"))})]))

(def parse-difficulty (comp #{:hard :normal :easy} keyword))
(def parse-volatility (comp #{:rare :balanced :frequent} keyword))

(defn- parse-aiskill-command [s]
  (when (string? s)
    (when (str/starts-with? s "!ai")
      (-> (update
           (->> (str/split s #" ")
                (map str/trim)
                (map str/lower-case)
                (remove str/blank?)
                (zipmap [:command :argument :volatility]))
           :argument parse-difficulty)
          (update :volatility parse-volatility)))))

(defn- handle-aiskill-command! [{:body/keys [text-start message user-type]}]
  (when (= :prefix user-type)
    (let [message                               (subs message text-start)
          {:keys [command argument volatility]} (parse-aiskill-command message)]
      (when command
        (apply println "Received !ai command with argument:" argument
               (when volatility ["and volatility" volatility])))
      (or
       (when (and command argument)
         (set-difficulty! argument volatility))
       (when command
         (let [report-message (str "AI preset: "
                                   (name @current-difficulty)
                                   ", try: !ai <difficulty> or !ai <difficulty> <volatility>")
               next-message   ".. difficulty choose [easy normal hard], volatility choose [frequent balanced rare]."]
           [(packets/msx {:message report-message})
            (packets/msx {:message next-message})]))))))

(defn- dispatch [client
                 {:header/keys [type player-id ucid]
                  :body/keys [player-type player-name new-ucid race-in-progress] :as packet}]
  (case type
    :sta        (swap! game-state assoc :race-in-progress? (pos? race-in-progress))
    :npl        (when (= :ai player-type) (new-ai-player! packet @current-difficulty))
    :pll        (swap! players dissoc player-id)
    :plp        nil ;; player stays in the race (pit garage) - player-id is retained, nothing to update
    :cpr        (rename-players-by-ucid! ucid player-name)
    ;; car handed to a different connection - only relevant if we're already tracking it
    :toc        (when (contains? @players player-id)
                  (swap! players assoc-in [player-id :ucid] new-ucid))
    :mso        (when-let [responses (handle-aiskill-command! packet)]
                  (run! (partial client/>! client) responses))
    (:lap :spx) (when (:race-in-progress? @game-state)
                  (when-let [response (some-> @players (get player-id) update-ai-skill!)]
                    (client/>! client response)))
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
       ;; Request initial state and player info - LFS only sends IS_NPL for players
       ;; joining *after* we connect otherwise. ReqI must be non-zero or LFS ignores.
       (client/>! client (packets/tiny {:request-info 1 :data :sst})) ;; STA packet for game state
       (client/>! client (packets/tiny {:request-info 1 :data :npl})) ;; NPL for existing players
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
  @volatility-override
  @game-state

  ;; To start the aiskill process
  (binding [logging/*packet-logging* false] ;; Control packet logging!
    (def aiskill-client (aiskill)))


  ;; To stop the client and aiskill process, simply call the stored function
  (aiskill-client)

  ;; Change difficulty and update all existing players (can also use /aiskill commands in-game)
  (set-difficulty! :easy :frequent)
  (set-difficulty! :normal :rare)
  (set-difficulty! :hard :balanced))
