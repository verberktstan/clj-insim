(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
;            [clj-insim.parsers :as parsers]
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

(def connections (atom {}))
;; @connections
;; (reset! connections {})

(defn- register-connection [{:keys [uniq-connection-id] :as connection}]
  (swap! connections assoc uniq-connection-id connection)
  nil)

(defn- unregister-connection [{:keys [uniq-connection-id] :as connection}]
  (swap! connections dissoc uniq-connection-id)
  nil)

(defn- check-handicaps [{:keys [handicap-mass uniq-connection-id] :as packet}]
  (if (>= handicap-mass 10)
    (packets/is-jrr (assoc packet :jrr-action (enums/jrr-action :spawn)))
    [(packets/is-jrr (assoc packet :jrr-action (enums/jrr-action :reject)))
     (packets/is-mtc uniq-connection-id 0 "You must respect 10kg ballast weight!")]
    ))

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

(defmethod dispatch :npl [{:keys [number-player] :as p}]
  (when (= number-player 0) ;; When this is a join request
    (check-handicaps p)))

(defn handler
  "Parse incoming packets from LFS and dispatch."
  [p]
  (let [{:keys [type] :as packet} (parse p)]
;    (newline) (println (str "== Received a " (name type) " from LFS ==")) (prn packet)
    (or (dispatch packet) (packets/is-tiny))))

(comment
  ;; Start insim from lfs by typing: "/insim 29999"
  (def lfs-client (client handler))
  (reset! lfs-client false)
)
