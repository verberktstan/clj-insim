(ns examples.aiskill
  (:gen-class)
  (:require [clj-insim.client :as client]
            [clj-insim.logging :as logging]
            [clj-insim.packets :as packets]
            [clojure.set :as set]
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

;; Shifts a preset's :min-skill/:max-skill down from the "pro" (no shift) baseline
(def ^:private skill-level-mapping {:pro 0 :advanced 1 :intermediate 2 :beginner 3 :newbie 4})

(defonce ^:private players (atom {})) ;; player-id -> {:player-name :player-id :ucid :preset :current-skill}
(defonce ^:private connections (atom {})) ;; ucid -> {:connection-name :ucid}
(defonce ^:private current-difficulty (atom :hard)) ;; tracks the active difficulty preset
(defonce ^:private current-skill-level (atom :pro)) ;; tracks the active skill-level cap
(defonce ^:private volatility-override (atom {:shuffle-odds-1-in nil}))

(defn- effective-preset-config
  "Returns `preset`'s config with :min-skill/:max-skill shifted down by the
   active skill-level cap, clamped so neither drops below 1."
  [preset]
  (let [shift (get skill-level-mapping @current-skill-level 0)]
    (-> (skill-presets preset)
        (update :min-skill #(max 1 (- % shift)))
        (update :max-skill #(max 1 (- % shift))))))

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


(defn checked-player-name
  "Only return the player-name if it matches the player's name in players state map."
  [players {:keys [player-id player-name]}]
  (some-> players (get player-id) :player-name #{player-name}))

(defn- update-ai-skill! [{:keys [player-id preset current-skill] :as props}]
  (let [preset-config (effective-preset-config preset)
        level         (calculate-next-skill current-skill @volatility-override preset-config)
        new-level?    (not= level current-skill)
        player-name   (checked-player-name @players props)]
    (when (and player-name level new-level?)
      (swap! players assoc-in [player-id :current-skill] level)
      (packets/mst {:message (str "/aiset " player-name " " level)}))))

(defn- new-connection! [{:header/keys [ucid] :body/keys [player-name admin]}]
  (swap! connections assoc ucid {:connection-name player-name :ucid ucid :admin (= admin :admin)}))

(defn- new-ai-player!
  "Starts tracking a newly joined AI player, assigning it a current
   difficulty preset and seeding its skill at that preset's `:max-skill`."
  [{:header/keys [player-id] :body/keys [player-name ucid]} preset]
  (swap! players assoc player-id
         {:player-name   player-name
          :player-id     player-id ;; duplicated so `update-ai-skill!` can `swap!` by id from the player map alone
          :ucid          ucid ;; tracked so a later IS_CPR rename (keyed by ucid, not player-id) can find this player
          :preset        preset
          :current-skill (:max-skill (effective-preset-config preset))}))

(defn- rename-players-by-ucid! [ucid player-name]
  (swap! connections assoc-in [ucid :connection-name] player-name)
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

(defn- clamp-player-skills
  "Brings any player's :current-skill down to `max-skill` if a new skill-level
   cap made it exceed the preset's new max."
  [players max-skill]
  (reduce-kv
   (fn clamp-player-skills* [players player-id {:keys [current-skill]}]
     (cond-> players
       (> current-skill max-skill) (assoc-in [player-id :current-skill] max-skill)))
   players
   players))

(def ^:private volatility-mapping {:rare 4 :balanced 3 :frequent 2})
(def ^:private volatility-reverse (set/map-invert volatility-mapping))

(defn- current-volatility-str []
  (if-let [override-n (:shuffle-odds-1-in @volatility-override)]
    (when-let [vol (volatility-reverse override-n)]
      (str " / " (name vol)))
    nil))

(defn- current-skill-level-str []
  (when (not= :pro @current-skill-level)
    (str " / " (name @current-skill-level))))

(defn- set-difficulty! [difficulty volatility skill-level]
  (when (contains? skill-presets difficulty)
    (println "Setting difficulty to" (name difficulty))
    (println "Setting volatility to" (or (some-> volatility name) "default"))
    (println "Setting skill cap to" (or (some-> skill-level name) "pro"))
    (reset! current-difficulty difficulty)
    (reset! current-skill-level (or skill-level :pro))
    (swap! players override-player-skills difficulty nil)
    (swap! players clamp-player-skills (:max-skill (effective-preset-config difficulty)))
    (reset! volatility-override {:shuffle-odds-1-in (get volatility-mapping volatility)}) ;; Could be nil or a keyword
    [(packets/mst {:message (str "ai difficulty set to " (name difficulty))})
     (packets/mst {:message (str "ai volatility set to " (or (some-> volatility name) "default"))})
     (packets/mst {:message (str "ai skill cap set to " (or (some-> skill-level name) "pro"))})]))

(def parse-difficulty (comp #{:hard :normal :easy} keyword))
(def parse-volatility (comp #{:rare :balanced :frequent} keyword))
(def parse-skill-level (comp #{:pro :advanced :intermediate :beginner :newbie} keyword))

(defn- parse-aiskill-command [s]
  (when (string? s)
    (when (str/starts-with? s "!ai")
      (let [[command & args] (->> (str/split s #" ")
                                   (map str/trim)
                                   (map str/lower-case)
                                   (remove str/blank?))]
        {:command     command
         :argument    (some parse-difficulty args)
         :volatility  (some parse-volatility args)
         :skill-level (some parse-skill-level args)}))))

(defn- handle-aiskill-command! [ucid {:body/keys [text-start message user-type]}]
  (when (= :prefix user-type)
    (let [message                                            (subs message text-start)
          {:keys [command argument volatility skill-level]} (parse-aiskill-command message)
          is-admin?                                          (get-in @connections [ucid :admin] false)]
      (when command
        (apply println "Received !ai command from UCID" ucid "admin:" is-admin? "with argument:" argument
               (concat (when volatility ["and volatility" volatility])
                       (when skill-level ["and skill cap" skill-level]))))
      (or
       (when (and command argument)
         (if is-admin?
           (set-difficulty! argument volatility skill-level)
           [(packets/msx {:message "Error: only admins can change AI difficulty"})]))
       (when command
         (let [report-message (str "AI preset: " (name @current-difficulty)
                                   (current-volatility-str)
                                   (current-skill-level-str)
                                   (when is-admin? " (type !ai <difficulty> to change)"))
               responses      [(packets/msx {:message report-message})]
               responses      (if is-admin?
                                (conj responses (packets/msx {:message "difficulty choose [easy normal hard], volatility choose [frequent balanced rare], skill cap choose [pro advanced intermediate beginner newbie]."}))
                                responses)]
           responses))))))

(defn- dispatch [client
                 {:header/keys [type player-id ucid]
                  :body/keys   [player-type player-name new-ucid] :as packet}]
  (let [send! (partial client/>! client)]
    (case type
      :ncn        (new-connection! packet)
      :npl        (when (= :ai player-type) (new-ai-player! packet @current-difficulty))
      :pll        (swap! players dissoc player-id)
      :plp        nil ;; player stays in the race (pit garage) - player-id is retained, nothing to update
      :cnl        (swap! connections dissoc ucid)
      :cpr        (rename-players-by-ucid! ucid player-name)
    ;; car handed to a different connection - only relevant if we're already tracking it
      :toc        (when (contains? @players player-id)
                    (swap! players assoc-in [player-id :ucid] new-ucid))
      :mso        (when-let [responses (handle-aiskill-command! ucid packet)]
                    (run! send! responses))
      (:lap :spx) (when-let [response (some-> @players (get player-id) update-ai-skill!)]
                    (send! response))
      :ver        (send! (packets/tiny {:request-info 1 :data :npl}))
      nil)))

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
  @connections
  @current-difficulty
  @current-skill-level
  @volatility-override

  ;; To start the aiskill process
  (binding [logging/*packet-logging* false] ;; Control packet logging!
    (def aiskill-client (aiskill)))

  (binding [logging/*packet-logging* false] ;; Control packet logging!
    (def aiskill-client
      (aiskill {:host "192.168.2.11" :port 29999})))

;; To stop the client and aiskill process, simply call the stored function
  (aiskill-client)

  ;; Change difficulty and update all existing players (can also use /aiskill commands in-game)
  (set-difficulty! :easy :frequent nil)
  (set-difficulty! :normal :rare nil)
  (set-difficulty! :hard :balanced :advanced))
