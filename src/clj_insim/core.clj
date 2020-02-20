(ns clj-insim.core
  (:require [clj-insim.packets :as packets]
            [clj-insim.queues :as queues]
            [clj-insim.read :as read]
            [clj-insim.write :as write]
            [clojure.java.io :as io])
  (:import [java.net Socket]))

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
      (queues/->queue out-queue (dispatch-fn packet)))))

(defn- write-output-packets!
  "Take packets from output queue and write to output stream"
  [out-queue output-stream]
  (when-let [packets (->> (seq @out-queue) (keep identity) seq)]
    (queues/reset-queue! out-queue)
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
  (enqueue! lfs-client (packets/mtc "Hello world!"))
  (stop! lfs-client)
)
