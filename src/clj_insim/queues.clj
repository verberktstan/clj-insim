(ns clj-insim.queues
  (:require [clj-insim.models.packet :as packet])
  (:refer-clojure :exclude [reset! pop! read]))

(defonce ^:private QUEUES
  {:in-queue (atom (clojure.lang.PersistentQueue/EMPTY))
   :out-queue (atom (clojure.lang.PersistentQueue/EMPTY))})

(def ^:private pop! #(swap! % pop))

(defn reset!
  ([]
   (doseq [queue (vals QUEUES)]
     (reset! queue)))
  ([queue]
   (clojure.core/reset! queue (clojure.lang.PersistentQueue/EMPTY))))

(defn enqueue!
  "Enqueue packet (or vector of packets) p to the queue.
  p must be associative (a hash-map or a vector)."
  ([p]
   (enqueue! (:out-queue QUEUES) p))
  ([queue p]
   (when (associative? p)
     (if (map? p)
       (swap! queue conj p)
       (apply swap! queue conj p)))))

(defn read [input-stream]
  (while (pos? (.available input-stream))
    (enqueue!
     (:in-queue QUEUES)
     (packet/read input-stream))))

(defn dispatch [dispatch-fn]
  (let [queue (:in-queue QUEUES)]
    (while (peek @queue)
      (let [packet (peek @queue)]
        (pop! queue)
        (enqueue! (dispatch-fn packet))))))

(defn write [output-stream]
  (let [queue (:out-queue QUEUES)]
    (when-let [packets (->> (seq @queue)
                            (keep identity)
                            seq)]
      (reset! queue)
      (packet/write output-stream packets))))
