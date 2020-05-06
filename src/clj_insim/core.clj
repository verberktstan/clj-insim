(ns clj-insim.core
  (:require [clj-insim.models.packet :as packet]
            [clj-insim.packets :as packets]
            [clj-insim.queues :as queues]
            [clj-insim.read :as read]
            [clj-insim.write :as write]
            [clojure.java.io :as io])
  (:import [java.net Socket]))

(defonce ^:private connections (atom {}))
(defonce ^:private players (atom {}))

(defn- register! [map-atom k v]
  (swap! map-atom assoc k v))

(defn- unregister! [map-atom k]
  (swap! map-atom dissoc k))

(defn- init! [atm out-queue  tiny-key]
  (reset! atm {})
  (queues/->queue out-queue (packets/tiny {:data tiny-key})))

(defn- manage-connections! [out-queue packet]
  (cond
    (packet/ncn? packet)
    (if (packet/reply? packet)
      (let [{:keys [connection-id] :as connection} (packet/ncn->connection packet)]
        (register! connections connection-id connection))
      (queues/->queue out-queue (packets/tiny {:data :ncn})))

    (packet/cnl? packet)
    (let [{:keys [connection-id]} (packet/cnl->connection packet)]
      (unregister! connections connection-id))))

(defn- manage-players! [out-queue packet]
  (cond
    (packet/npl? packet)
    (if (packet/reply? packet)
      (let [{:keys [player-id] :as player} (packet/npl->player packet)]
        (register! players player-id player))
      (queues/->queue out-queue (packets/tiny {:data :npl})))

    (packet/pll? packet)
    (let [{:keys [player-id]} (packet/pll->player packet)]
      (unregister! players player-id))))

(defn- init-connections-and-players! [out-queue]
  (init! connections out-queue :ncn)
  (init! players out-queue :npl))

(defn- maintain-connection! [out-queue packet]
  (when (packet/tiny-none? packet)
    (packets/tiny)))

(defn- read-input-packets!
  "Read packets from input stream and put them on queue"
  [input-stream queue]
  (when-let [packets (read/packets input-stream)]
    (queues/->queue queue packets)))

(defn- dispatch!
  "Call dispatch-fn on packets on input queue, put result on output queue"
  [in-queue dispatch-fn out-queue]
  (while (seq @in-queue)
    (when-let [packet (queues/peek-and-pop! in-queue)]
      (maintain-connection! out-queue packet)
      (manage-connections! out-queue packet)
      (manage-players! out-queue packet)
      (queues/->queue
       out-queue
       (try
         (dispatch-fn packet)
         (catch Exception e (println e)))))))

(defn- write-output-packets!
  "Take packets from output queue and write to output stream"
  [out-queue output-stream]
  (when-let [packets (queues/peek-and-pop-all! out-queue)]
    (write/packets output-stream packets)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public funtions

(defn enqueue! [{:keys [enqueue!] :as client} packet]
  (enqueue! packet))

(defn stop!
  "Enqueues the insim close packet to close connection and stops the loop"
  [{:keys [running sleep-interval] :as client}]
  (enqueue! client (packets/tiny {:request-info 0 :data :close}))
  (Thread/sleep (* sleep-interval 2))
  (reset! running false))

(defn get-connection
  ([] @connections)
  ([ucid] (get @connections ucid)))

(defn get-player
  ([] @players)
  ([plid] (get @players plid)))

(defn packet-type [{::packet/keys [header]}]
  (:type header))

(defn client
  "Opens a socket and reads packets from input stream to in-queue, calls
  dispatch-fn on each packet, and writes packets from out-queue to output
  stream. Returns a map representing the client (containing :running, :enqueue!
  and :sleep-interval)"
  ([]
   (client println))
  ([dispatch-fn]
   (client {} (packets/insim-init) dispatch-fn))
  ([{:keys [host port sleep-interval]} init-packet dispatch-fn]
   (let [running (atom true)
         {:keys [in-queue out-queue enqueue!]} (queues/make init-packet)]
     (init-connections-and-players! out-queue)
     (future
       (with-open [socket (Socket. (or host "127.0.0.1") (or port 29999))
                   output-stream (io/output-stream socket)
                   input-stream (io/input-stream socket)]
         (while @running
           (read-input-packets! input-stream in-queue)
           (dispatch! in-queue dispatch-fn out-queue)
           (write-output-packets! out-queue output-stream)
           (Thread/sleep (or sleep-interval 100)))))
     {:running running
      :enqueue! enqueue!
      :sleep-interval (or sleep-interval 100)})))

(comment
  (def lfs-client (client))
  (enqueue! lfs-client (packets/mst "Hello world!"))
  (enqueue! lfs-client (packets/mtc "Hello world!"))
  (enqueue! lfs-client (packets/tiny))
  (stop! lfs-client)

  @connections
  @players
)
