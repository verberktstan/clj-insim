(ns clj-insim.core
  (:require [clj-insim.connection :as connection]
            [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.player :as player]
            [clj-insim.parser :refer [parse]]
            [clj-insim.socket :as socket]
            [clj-insim.util :as util]
            [clojure.string :refer [upper-case]]))

(defn- check-version
  "Returns a welcome message if LFS version of host is 0.6T and insim-version is 8. Else returns an IS_TINY packet that closes the connection."
  [{:keys [version insim-version]}]
  (if (and (= version "0.6T")
           (= insim-version 8))
    [(packets/is-msl "Warm welcome from clj-insim!")]
    [(packets/is-tiny {:data-key :close})]))

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

(defmethod dispatch :tiny [{:keys [sub-type]}]
  (when (= sub-type :none)
    (println "Sent a IS_TINY packet to maintain connection")
    [(packets/is-tiny)]))

(defmethod dispatch :npl [{:keys [number-player] :as npl}]
  (when (= 0 number-player)
    [(packets/is-jrr (assoc npl :jrr-action (enums/jrr-action :spawn)))]))

(defn default-handler
  "Returns a function that parses and dispatches incoming packets from LFS.
  (default-handler {:print-packets? true}) ;; The handler will print all incoming packets to the REPL."
  ([]
   (default-handler nil))
  ([{:keys [print-packets?]}]
   (fn [{:keys [type] :as packet}]
     (when print-packets?
       (newline) (println (str "== Received a IS_" (upper-case (name type)) " from LFS ==")) (prn packet))
     (seq (dispatch packet)))))

(defn client
  "Creates a new tcp client, returns an atom representing the running state of the client; reset! this atom to false to stop the client. Specify :host, :port and :interval in options, connects to localhost:29999 by default."
  ([]
   (client (default-handler {:print-packets? false}) {:debug true}))
  ([handler]
   (client handler nil))
  ([handler options]
   (socket/client handler options)))

(comment
  ;; Start insim from lfs by typing: "/insim 29999"
  (def lfs-client (client))
  (reset! lfs-client false)
)
