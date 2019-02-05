(ns clj-insim.core
  (:require [clj-insim.connection :as connection]
            [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.player :as player]
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

(defmulti dispatch :type)
(defmethod dispatch :default [_] nil)
(defmethod dispatch :ver [p]
  (check-version p))


;;;;; Registration of connections
(defmethod dispatch :ncn [p] (connection/dispatch-ncn p {:notify-host? true}))
(defmethod dispatch :cnl [p] (connection/dispatch-cnl p {:notify-host? true}))


;;;;; Registration of players
(defmethod dispatch :npl [p] (player/dispatch-npl p {:notify-host? true}))
(defmethod dispatch :pll [p] (player/dispatch-pll p {:notify-host? true}))


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
