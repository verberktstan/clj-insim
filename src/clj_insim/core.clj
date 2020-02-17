(ns clj-insim.core
  (:require [clj-insim.parse :as parse]
            [clj-insim.codecs :as codecs]
            [clj-insim.packets :as packets]
            [clj-insim.models.packet :as packet]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [marshal.core :as m])
  (:refer-clojure :exclude [pop!])
  (:import [java.net Socket]))

(defonce ^:private QUEUES
  {:in-queue (atom (clojure.lang.PersistentQueue/EMPTY))
   :out-queue (atom (clojure.lang.PersistentQueue/EMPTY))})

(defn- pop! [queue]
  (swap! queue pop))

(defn- reset-queue! [queue-key]
  (when-let [queue (get QUEUES queue-key)]
    (clojure.core/reset! queue (clojure.lang.PersistentQueue/EMPTY))))

(defn- reset-queues! []
  (doseq [queue-key (keys QUEUES)]
    (reset-queue! queue-key)))

(defn- ->queue
  "Put packet(s) p on a queue."
  [queue-key p]
  (when-let [queue (get QUEUES queue-key)]
    (cond
      (s/valid? ::packet/model p)
      (swap! queue conj p)

      (s/valid? (s/coll-of ::packet/model) p)
      (apply swap! queue conj p)

      :else
      (println
       (or (s/explain-data ::packet/model p)
           (s/explain-data (s/coll-of ::packet/model) p))))))

(defn- input-stream->in-queue!
  "Read a single packet from the input-stream and enqueue it (on the input queue)"
  [input-stream]
  (while (pos? (.available input-stream))
    (->queue :in-queue (packet/read input-stream))))

(defn- dispatch!
  "Call dispatch-fn on all packets (in input queue)
  and enqueue the result (on the output queue)"
  [dispatch-fn]
  (let [queue (:in-queue QUEUES)]
    (while (seq @queue)
      (let [packet (peek @queue)]
        (pop! queue)
        (when (s/valid? ::packet/model packet)
          (->queue :out-queue (dispatch-fn packet)))))))

(defn- out-queue->output-stream!
  "Write all queued packets to the output-stream (if any)."
  [output-stream]
  (let [queue (:out-queue QUEUES)]
    (when-let [packets (->> (seq @queue)
                            (keep identity)
                            seq)]
      (reset-queue! :out-queue)
      (packet/write output-stream packets))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; State

(defonce version (atom nil))
(defonce state (atom nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions

(defn enqueue!
  "Enqueue packet (or vector of packets) p to the queue.
  p must be associative (a hash-map or a vector)."
  [p]
  (->queue :out-queue p))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Dispatching packets

(defmulti dispatch #(get-in % [::packet/header :type]))

(defmethod dispatch :default [packet]
  nil)

(defmethod dispatch :ver [{::packet/keys [header body]}]
  (when (#{1} (:request-info header))
    (reset! version (dissoc body :spare))))

(defmethod dispatch :tiny [packet]
  (when (packet/tiny-none? packet)
    (packets/tiny)))

(defmethod dispatch :sta [{::packet/keys [body]}]
  (reset! state (dissoc body :spare2 :spare3)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn client
  "Opens a tcp socket and reads packets from input stream to in-queue,
  and writes packets from out-queue to output stream."
  ([]
   (client nil))
  ([{:keys [host port sleep-interval dispatch-fn]}]
   (let [running (atom true)]
     (reset-queues!)
     (enqueue! (packets/insim-init))
     (future
       (with-open [socket (Socket. (or host "127.0.0.1") (or port 29999))
                   output-stream (io/output-stream socket)
                   input-stream (io/input-stream socket)]
         (while @running
           (input-stream->in-queue! input-stream)
           (dispatch! dispatch-fn)
           (out-queue->output-stream! output-stream)
           (Thread/sleep (or sleep-interval 100)))))
     running)))

(comment
  (def lfs-client (client {:dispatch-fn dispatch}))
  (enqueue! (packets/mst "Hello world!"))
  (enqueue! {:test "packet"})
  (enqueue! nil)
  (enqueue! (packets/tiny {:request-info 0 :data :close}))
  (reset! lfs-client false)

  (remove-all-methods dispatch)

  @version
  @state
)
