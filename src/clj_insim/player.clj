(ns clj-insim.player
  (:require [clj-insim.packets :as packets]))

(defonce players (atom {}))
;; @players
;; (count @players)
;; (reset! players nil)

(defn register! [{:keys [player-id number-player] :as player} {:keys [notify-host? check-total-players?]}]
  (when (not (contains? @players player-id)) ;; when player NOT already registered
    (swap! players assoc player-id (dissoc player :size :type :reqi))
    [(when (and check-total-players? (not= (count @players) number-player)) ;; When not in sync
       (reset! players nil)              ;; Reset player map
       (packets/is-tiny {:data-key :npl})) ;; Request all players
     (when notify-host? (packets/is-msl (str "clj-insim: player " player-id " registered!")))]))

(defn unregister! [{:keys [player-id]} {:keys [notify-host?]}]
  (swap! players dissoc player-id)
  (when notify-host? (packets/is-msl (str "clj-insim: player " player-id " UNregistered!"))))
