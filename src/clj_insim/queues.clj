(ns clj-insim.queues
  (:require [clj-insim.models.packet :as packet]
            [clojure.spec.alpha :as s])
  (:refer-clojure :exclude [reset! pop!]))

(defonce ^:private QUEUES
  {:in-queue (atom (clojure.lang.PersistentQueue/EMPTY))
   :out-queue (atom (clojure.lang.PersistentQueue/EMPTY))})

(defn- pop! [queue]
  (swap! queue pop))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public interface

(defn reset!
  ([]
   (doseq [queue-key (keys QUEUES)]
     (reset! queue-key)))
  ([queue-key]
   (when-let [queue (get QUEUES queue-key)]
     (clojure.core/reset! queue (clojure.lang.PersistentQueue/EMPTY)))))

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

(defn enqueue!
  "Enqueue packet (or vector of packets) p to the queue.
  p must be associative (a hash-map or a vector)."
  [p]
  (->queue :out-queue p))

(defn read!
  "Read a single packet from the input-stream and enqueue it (on the input queue)"
  [input-stream]
  (while (pos? (.available input-stream))
    (->queue :in-queue (packet/read input-stream))))

(defn dispatch!
  "Call dispatch-fn on all packets (in input queue)
  and enqueue the result (on the output queue)"
  [dispatch-fn]
  (let [queue (:in-queue QUEUES)]
    (while (seq @queue)
      (let [packet (peek @queue)]
        (pop! queue)
        (when (s/valid? ::packet/model packet)
          (->queue :out-queue (dispatch-fn packet)))))))

(defn write!
  "Write all queued packets to the output-stream (if any)."
  [output-stream]
  (let [queue (:out-queue QUEUES)]
    (when-let [packets (->> (seq @queue)
                            (keep identity)
                            seq)]
      (reset! :out-queue)
      (packet/write output-stream packets))))
