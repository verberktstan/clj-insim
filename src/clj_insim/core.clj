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

(def ^:private DEBUG false)

(defn- pop! [queue]
  (let [packet (peek @queue)]
    (swap! queue pop)
    packet))

(defn- reset-queue! [queue]
  (reset! queue (clojure.lang.PersistentQueue/EMPTY)))

(defn- ->queue
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

(defn- read-packets
  "Returns packets (based on all available bytes) read from input-stream"
  [input-stream]
  (loop [available-bytes (.available input-stream)
         result []]
    (if (not (pos? available-bytes))
      (seq result)
      (let [packet (packet/read input-stream)]
        (recur (.available input-stream) (conj result packet))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; State

(defonce version (atom nil))
(defonce state (atom nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Dispatching packets

(defmulti dispatch #(get-in % [::packet/header :type]))

(defmethod dispatch :default [packet]
  (when DEBUG
    (newline)
    (println "--===####===--")
    (println packet)))

(defmethod dispatch :ver [{::packet/keys [header body]}]
  (when (#{1} (:request-info header))
    (reset! version (dissoc body :spare))))

(defmethod dispatch :tiny [packet]
  (when (packet/tiny-none? packet)
    (packets/tiny)))

(defmethod dispatch :sta [{::packet/keys [body]}]
  (reset! state (dissoc body :spare2 :spare3)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialisation

(defn- make-queues
  "Encloses the required queues and a function for enqueue'ing packets on the
  output stream. Returns a map containing :in-queue, :out-queue & :enqueue-fn."
  []
  (let [in-queue (atom (clojure.lang.PersistentQueue/EMPTY))
        out-queue (atom (clojure.lang.PersistentQueue/EMPTY))
        enqueue-fn #(->queue out-queue %)]
    {:in-queue in-queue
     :out-queue out-queue
     :enqueue-fn enqueue-fn}))

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
   (client nil))
  ([{:keys [host port sleep-interval dispatch-fn]}]
   (let [running (atom true)
         {:keys [in-queue out-queue enqueue-fn] :as queues} (make-queues)]
     (enqueue-fn (packets/insim-init))
     (future
       (with-open [socket (Socket. (or host "127.0.0.1") (or port 29999))
                   output-stream (io/output-stream socket)
                   input-stream (io/input-stream socket)]
         (while @running
           ;; Read packets from the input stream
           (when-let [packets (read-packets input-stream)]
             ;; Enqueue the packets to the input queue
             (->queue in-queue packets))

           ;; Call dispatch-fn on all queue'd packets
           (while (seq @in-queue)
             (when-let [packet (pop! in-queue)]
               ;; Enqueue the packets to the output queue
               (->queue out-queue (dispatch-fn packet))))

           ;; Take all packets from the output queue
           (when-let [packets (->> (seq @out-queue)
                                   (keep identity)
                                   seq)]
             (reset-queue! out-queue)
             ;; Write the packets to the output stream
             (packet/write output-stream packets))

           (Thread/sleep (or sleep-interval 100)))))
     {:running running
      :enqueue! enqueue-fn
      :sleep-interval (or sleep-interval 100)})))

(comment
  (def lfs-client (client {:dispatch-fn dispatch
                           :sleep-interval 250}))
  (enqueue! lfs-client (packets/mst "Hello world!"))
  (enqueue! lfs-client {:test "packet"})
  (enqueue! lfs-client nil)
  (stop! lfs-client)

  (remove-all-methods dispatch)

  @version
  @state
)
