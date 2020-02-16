
(ns clj-insim.core
  (:require [clj-insim.parse :as parse]
            [clj-insim.codecs :as codecs]
            [clj-insim.connections :refer [ncn->connection]]
            [clj-insim.packets :as packets]
            [clj-insim.models.packet :as packet]
            [clj-insim.queues :refer [enqueue!] :as queues]
            [clojure.java.io :as io]
            [marshal.core :as m])
  (:import [java.net Socket]))

(def DEBUG true)

(defonce connections (atom nil))
(defonce players (atom nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Dispatching packets

(defmulti dispatch #(get-in % [::packet/header :type]))

(defmethod dispatch :default [packet]
  (when false #_DEBUG
        (newline)
        (println "Default dispatch: " packet)))

(defmethod dispatch :tiny [packet]
  (when (packet/tiny-none? packet)
    [(when DEBUG (packets/mst "Maintaining connection..!"))
     (packets/tiny {:data :none})]))

(defmethod dispatch :ncn [packet]
  (let [{:connection/keys [id] :as connection} (ncn->connection packet)]
    (swap! connections assoc id connection)
    (packets/mst (str "New connection: " connection))))

(defmethod dispatch :cnl [packet]
  (newline)
  (println "--== CNL packet ==--")
  (println packet))

#_(defn npl->player [{::packet/keys [header body]}]
  #:player{:id (:data header)
           :connection-id (:connection-id body)
           :type (:player-type body)
           :flags (:flags body)
           :name (:player-name body)
           :plate (:plate body)
           :car-name (:car-name body)
           :skin-name (:skin-name body)
           :tyres (:tyres body)
           :handicap #:handicap{:mass (:handicap-mass body)
                                :restriction (:handicap-restriction body)}
           :model (:model body)
           :passengers (:passengers body)
           :setup-flags {:setup-flags body}
           :num-player (:num-player body)})

#_(defmethod dispatch :npl [packet]
  (println "Dispatching NPL: " packet)
  (let [{:player/keys [id] :as player} (npl->player packet)]
    (swap! players assoc id player)
    (packets/mst (str "New player: " player))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn client
  "Opens a tcp socket and reads packets from input stream to in-queue,
  and writes packets from out-queue to output stream."
  ([]
   (client nil))
  ([{:keys [host port sleep-interval dispatch-fn]}]
   (let [running (atom true)]
     (queues/reset!)
     (reset! connections nil)
     (enqueue! (packets/insim-init))
     (future
       (with-open [socket (Socket. (or host "127.0.0.1") (or port 29999))
                   output-stream (io/output-stream socket)
                   input-stream (io/input-stream socket)]
         (while @running
           (queues/read input-stream)
           (queues/dispatch dispatch-fn)
           (queues/write output-stream)
           (Thread/sleep (or sleep-interval 1000)))))
     running)))

(comment
  (enqueue! (packets/mst "Hello world!"))

  (enqueue! (packets/request-ncn))
  (enqueue! (packets/request-npl))

  @connections
  @players

  (def lfs-client (client {:dispatch-fn dispatch
                           :sleep-interval 100}))
  (enqueue! (packets/close))
  (reset! lfs-client false)

  (remove-all-methods dispatch)
)
