(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parser :refer [parse]]
            [clj-insim.socket :refer [client]]
            [clj-insim.util :as util]))

(defn- check-version
  "Returns a welcome message if LFS version of host is 0.6T and insim-version is 8. Else returns an IS_TINY packet that closes the connection."
  [{:keys [version insim-version]}]
  (if (and (= version "0.6T")
           (= insim-version 8))
    (packets/is-msl "Warm welcome from clj-insim!")
    (packets/is-tiny {:data-key :close})))

(defonce connections (atom {}))
;; @connections
;; (count @connections)
;; (reset! connections nil)

(defonce players (atom {}))
;; @players
;; (count @players)
;; (reset! players nil)

(defn- register-connection! [{:keys [uniq-connection-id total] :as connection} connections {:keys [check-total-connections? notify-host?]}]
  (swap! connections assoc uniq-connection-id (dissoc connection :type :size :reqi :spare :total))
  [(when (and check-total-connections? (not= (count @connections) total)) ;; when not in sync
     (reset! connections nil) ;; Reset connection map
     (packets/is-tiny {:data-key :ncn})) ;; Request all connections
   (when notify-host? (packets/is-msl (str "clj-insim: connection " uniq-connection-id " registered!")))])

(defn- unregister-connection! [{:keys [uniq-connection-id] :as connection} connections {:keys [notify-host?]}]
  (swap! connections dissoc uniq-connection-id)
  (when notify-host? (packets/is-msl (str "clj-insim: connection " uniq-connection-id " UNregistered!"))))


(defn- register-player! [{:keys [player-id number-player] :as player} players {:keys [notify-host? check-total-players?]}]
  (swap! players assoc player-id (dissoc player :size :type :reqi))
  [(when (and check-total-players? (not= (count @players) number-player)) ;; When not in sync
     (reset! players nil)              ;; Reset player map
     (packets/is-tiny {:data-key :npl})) ;; Request all players
   (when notify-host? (packets/is-msl (str "clj-insim: player " player-id " registered!")))])

(defn- unregister-player! [{:keys [player-id]} players {:keys [notify-host?]}]
  (swap! players dissoc player-id)
  (when notify-host? (packets/is-msl (str "clj-insim: player " player-id " UNregistered!"))))

;;;;; public functions

(defmulti dispatch :type)
(defmethod dispatch :default [_] nil)
(defmethod dispatch :ver [p]
  (check-version p))


;;;;; Registration of connections

(defmethod dispatch :ncn [{:keys [reqi] :as p}]
  ;; Register the connection and check the total connection count if this is not a response to TINY_NCN
  (register-connection! p connections {:check-total-connections? (= reqi 0) :notify-host? true}))

(defmethod dispatch :cnl [p]
  (unregister-connection! p connections {:notify-host? true}))


;;;;; Registration of players

(defmethod dispatch :npl [{:keys [player-id reqi number-player reqi] :as p}]
  (when (and (not (contains? @players player-id)) ;; when player NOT already registered
             (not (zero? number-player))) ;; and this is NOT a join request
    (register-player! p players {:check-total-players? (= reqi 0) :notify-host? true})))

(defmethod dispatch :pll [p]
  (unregister-player! p players {:notify-host? true}))


(defn handler
  "Parse incoming packets from LFS and dispatch."
  [p {:keys [print-packets?]}]
  (let [{:keys [type] :as packet} (parse p)]
    (when print-packets?
      (newline) (println (str "== Received a " (name type) " from LFS ==")) (prn packet))
    (or (dispatch packet) (packets/is-tiny))))

(comment
  ;; Start insim from lfs by typing: "/insim 29999"
  (def lfs-client (client #(handler % {:print-packets? true})))
  (reset! lfs-client false)
)
