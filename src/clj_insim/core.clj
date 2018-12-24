(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parsers :as parsers]
            [clj-insim.socket :as socket]
            [clj-insim.util :as util]))

(defonce race-in-progress? (atom :none))

(defn- welcome
  "Returns an IS_MST packet with a warm welcom message from clj-insim."
  []
  (packets/is-mtc 255 0 "Welcome to clj-insim!"))

(defn- check-version
  "Returns a welcome message if LFS version of host is 0.6T and insim-version is greater than 7. Else returns an IS_TINY packet that closes the connection."
  [{:keys [version insim-version]}]
  (if (and (= version "0.6T")
           (>= insim-version 7))
    (welcome)
    (packets/is-tiny {:data-key :close})))

(defn- update-state
  "Returns an IS_MST packet with a notification of the changed race-in-progress state. If race-in-progress is not changed, returns nil."
  [{:keys [race-in-progress]}]
  (when (not= @race-in-progress? race-in-progress)
    (do
      (reset! race-in-progress? race-in-progress)
      (packets/is-mtc 255 0 (str "Race in progress: " (name race-in-progress))))))

(defn simple-commands [{:keys [message text-start user-type player-id uniq-connection-id]}]
  (when (= user-type :prefix)
    (let [command (subs message text-start)]
      (case command
        "!mst" (packets/is-mst "Echo!")
        "!mtc" (packets/is-mtc uniq-connection-id player-id "Echo, baby!")
        "!ubermessage" [(packets/is-mst "Uberecho!") (packets/is-mtc uniq-connection-id player-id "Uberecho, baby!")]
        nil))))

;; Specify dispatchers for each type of packet
(def ^:private dispatchers
  {:sta update-state
   :ver check-version
   :mso simple-commands})

(defn- dispatch
  "Returns the result of (dispachters (:type packet)) applied to the incoming packet. Prints incoming packet as side effect."
  [{:keys [type] :as packet}]
  (when packet
    (println (str "\n== clj-insim received a " (name type) " packet from LFS =="))
    (prn packet)
    (when-let [f (type dispatchers)]
      (f packet))))

;;;;; Public functions

(defn parse
  ([packet]
   (let [type-key (-> packet first enums/isp-key)]
     (parse type-key packet)))
  ([type-key packet]
   (let [protocol (parsers/make-protocol type-key)]
     (when protocol
       (reduce parsers/parse-bytes {:coll packet} protocol)))))

(defn test-handler
  "Consumes a binary packet from LFS InSim, returns a packet by dispatching. If dispatch returns nil, will return a IS_TINY packet to maintain connection."
  [packet]
  (or (-> packet parse dispatch) (packets/is-tiny)))

(defn client [handler & {:keys [host port isi-options]}]
  (let [running (atom true)]
    (future
      (with-open [socket (socket/make-socket (or host socket/HOST) (or port socket/PORT))
                  _ (socket/send-packet socket (packets/is-isi isi-options))]
        (while @running
          (let [in (socket/receive-packet socket)
                out (handler in)]
            (socket/send-packet socket out)))))
    running))

(comment
  (def lfs-client (client test-handler))
  (reset! lfs-client false)
)
