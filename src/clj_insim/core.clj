(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
;            [clj-insim.parsers :as parsers]
            [clj-insim.parser :refer [parse]]
            [clj-insim.socket :refer [client]]
            [clj-insim.util :as util]))

(def championship (atom {}))
;; @championship
;; (get @championship "Boer Tarrel")
;; (reset! championship {})

(defn- check-version
  "Returns a welcome message if LFS version of host is 0.6T and insim-version is 8. Else returns an IS_TINY packet that closes the connection."
  [{:keys [version insim-version]}]
  (if (and (= version "0.6T")
           (= insim-version 8))
    (packets/is-msl "Warm welcome from clj-insim!")
    (packets/is-tiny {:data-key :close})))

(def connections (atom {}))
;; @connections
;; (reset! connections {})

(def players (atom {}))
;; @players
;; (reset! players {})

(defn- register-connection [{:keys [uniq-connection-id] :as connection}]
  (swap! connections assoc uniq-connection-id connection)
  (packets/is-msl (str "Connection " uniq-connection-id " registered!")))

(defn- unregister-connection [{:keys [uniq-connection-id] :as connection}]
  (swap! connections dissoc uniq-connection-id)
  (packets/is-msl (str "Connection " uniq-connection-id " UNregistered!")))

(defn- register-player [{:keys [player-id] :as player}]
  (swap! players assoc player-id player)
  (packets/is-msl (str "Player " player-id " registered!")))

(defn- unregister-player [{:keys [player-id] :as player}]
  (swap! players dissoc player-id)
  (packets/is-msl (str "Player " player-id " UNregistered!")))

(defn- create-championship-player! [{:keys [player-name uniq-connection-id]}]
  (swap! championship assoc player-name {:mass 0})
  (packets/is-mtc uniq-connection-id 0 "Welcome to the championship!"))

(defn- check-handicaps [{:keys [handicap-mass uniq-connection-id player-name] :as npl-packet} connections championship]
  (let [{:keys [user-name] :as connection} (get @connections uniq-connection-id)
        {:keys [mass]} (get @championship player-name)]
    (cond
      ;; When mass / championship player are not present, assoc a new entry to the championship
      (nil? mass)
      [(create-championship-player! npl-packet)
       (packets/is-jrr (assoc npl-packet :jrr-action (enums/jrr-action :spawn)))]

      ;; When handicap-mass is creater than mass, spawn player
      (and mass (>= handicap-mass mass))
      [(packets/is-jrr (assoc npl-packet :jrr-action (enums/jrr-action :spawn)))
       (packets/is-mtc uniq-connection-id 0 (str "You respect the required ballast weight (" handicap-mass "kg / " (or mass 0) "kg)"))]

      ;; Else reject player
      :else
      [(packets/is-jrr (assoc npl-packet :jrr-action (enums/jrr-action :reject)))
       (packets/is-mtc uniq-connection-id 0 (str "You must respect ballast weight (" mass "kg)"))])))

;;;;; public functions

(defmulti dispatch :type)
(defmethod dispatch :default [_] nil)
(defmethod dispatch :ver [p]
  (check-version p))

(defmethod dispatch :cnl [{:keys [reqi] :as p}]
  (when (= reqi 0)
    (unregister-connection p)))
(defmethod dispatch :ncn [{:keys [reqi] :as p}]
  (when (= reqi 0)
    (register-connection p)))

(defmethod dispatch :npl [{:keys [number-player driver-model] :as p}]
  (if (= number-player 0) ;; When this is a join request
    (check-handicaps p connections championship)
    (register-player p)))

(defmethod dispatch :pll [{:keys [player-id] :as p}]
  (unregister-player p))

(defn handler
  "Parse incoming packets from LFS and dispatch."
  [p]
  (let [{:keys [type] :as packet} (parse p)]
    (newline) (println (str "== Received a " (name type) " from LFS ==")) (prn packet)
    (or (dispatch packet) (packets/is-tiny))))

(comment
  ;; Start insim from lfs by typing: "/insim 29999"
  (def lfs-client (client handler))
  (reset! lfs-client false)
)
