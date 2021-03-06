(ns clj-insim.queues
  (:require [clj-insim.models.packet :as packet]
            [clojure.spec.alpha :as s]))

(def ^:private DEBUG false)

(defn- reset-queue! [queue]
  (reset! queue (clojure.lang.PersistentQueue/EMPTY)))

(defn peek-and-pop! [queue]
  (let [packet (peek @queue)]
    (swap! queue pop)
    packet))

(defn peek-and-pop-all! [queue]
  (when-let [packets (->> (seq @queue) (filter identity) (seq))]
    (reset-queue! queue)
    packets))

(defn ->queue
  "Put packet(s) p on a queue."
  [queue p]
  (cond
    (s/valid? ::packet/model p)
    (swap! queue conj p)

    (s/valid? (s/coll-of ::packet/model) p)
    (apply swap! queue conj p)

    :else
    (when DEBUG
      (do
        (newline)
        (println "Invalid packet enqueued!")
        (println
         (or (s/explain-data ::packet/model p)
             (s/explain-data (s/coll-of ::packet/model) p)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialisation

(defn make
  "Encloses the required queues and a function for enqueue'ing packets on the
  output stream. Returns a map containing :in-queue, :out-queue & :enqueue-fn."
  ([]
   (make nil))
  ([init-packet]
   (let [in-queue (atom (clojure.lang.PersistentQueue/EMPTY))
         out-queue (atom (clojure.lang.PersistentQueue/EMPTY))
         enqueue! #(->queue out-queue %)]
     (when init-packet
       (enqueue! init-packet))
     {:in-queue in-queue
      :out-queue out-queue
      :enqueue! enqueue!})))
