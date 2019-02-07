(ns clj-insim.player
  (:require [clj-insim.packets :as packets]
            [clj-insim.util :as util]))

(defonce players (atom {}))
;; @players
;; (count @players)
;; (reset! players nil)

(defn register!
  [{:keys [player-id num-players] :as player} {:keys [notify-host? check-total-players?]}]
  (when (not (contains? @players player-id)) ;; when player NOT already registered
    (swap! players assoc player-id (dissoc player :size :type :reqi))
    [(when (and check-total-players? (not= (count @players) num-players)) ;; When not in sync
       (reset! players nil)              ;; Reset player map
       (packets/is-tiny {:data-key :npl})) ;; Request all players
     (when notify-host? (packets/is-msl (str "clj-insim: player " player-id " registered!")))]))

(defn unregister! [{:keys [player-id]} {:keys [notify-host?]}]
  (swap! players dissoc player-id)
  (when notify-host? [(packets/is-msl (str "clj-insim: player " player-id " UNregistered!"))]))

(defn dispatch-npl
  "Basic dispath fn for a IS_NPL packet. Registers a player.
  (dispatch-npl {:type :npl :num-players 2 :reqi 1 :player-id 1} :notify-host? true)"
  ([npl-packet]
   (dispatch-npl npl-packet nil))
  ([{:keys [reqi] :as npl-packet} options]
   (when (not (util/join-request? npl-packet)) ;; and this is NOT a join request
     (register! npl-packet (merge {:check-total-players? (zero? reqi)} options)))))

(defn dispatch-pll
  "Basic dispatcher for a IS_PLL packet. Unregisters a player.
  (dispatch-pll {:type :pll :player-id 1} :notify-host? true)"
  ([pll-packet]
   (dispatch-pll pll-packet nil))
  ([pll-packet options]
   (unregister! pll-packet options)))
