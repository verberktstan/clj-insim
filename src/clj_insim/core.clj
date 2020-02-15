
(ns clj-insim.core
  (:require [clj-insim.parse :as parse]
            [clj-insim.codecs :as codecs]
            [clj-insim.packets :as packets]
            [clj-insim.models.packet :as packet]
            [marshal.core :as m])
  (:import [java.net Socket]))

(def DEBUG true)

(defonce ^:private QUEUES
  {:in-queue (atom (clojure.lang.PersistentQueue/EMPTY))
   :out-queue (atom (clojure.lang.PersistentQueue/EMPTY))})

(defn enqueue!
  "Enqueue packet (or vector of packets) p to the queue.
  p must be associative (a hash-map or a vector)."
  ([p]
   (enqueue! (:out-queue QUEUES) p))
  ([queue p]
   (when (associative? p)
     (cond (map? p)
           (swap! queue conj p)
           (vector? p)
           (doseq [packet p]
             (swap! queue conj packet))))))

(defonce connections (atom nil))
(defonce players (atom nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Dispatching packets

(defmulti dispatch #(get-in % [::packet/header :type]))

(defmethod dispatch :default [packet]
  (when DEBUG
    (newline)
    (println "Default dispatch: " packet)))

(defmethod dispatch :tiny [{::packet/keys [header]}]
  (when (and (zero? (:request-info header))
             (= (:data header) :none))
    [(packets/mst "Maintaning connection..!") (packets/tiny {:data :none})]))

(defn ncn->connection [{::packet/keys [header body]}]
  #:connection{:id (:data header)
               :user-name (:user-name body)
               :player-name (:player-name body)
               :admin? (when (= (:admin body) 1) true)
               :flags (:flags body)})

(defmethod dispatch :ncn [packet]
  (println "Dispatching NCN: " packet)
  (let [{:connection/keys [id] :as connection} (ncn->connection packet)]
    (swap! connections assoc id connection)
    (packets/mst (str "New connection: " (doto connection println)))))

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
    (packets/mst (str "New player: " (doto player println)))))

(defn- read-in-queue [input-stream in-queue]
  (while (pos? (.available input-stream))
    (enqueue! in-queue (packet/read input-stream))))

(defn- dispatch-in-queue [{:keys [in-queue out-queue]}]
  (when-let [packets (seq @in-queue)]
    (reset! in-queue (clojure.lang.PersistentQueue/EMPTY))
    (doseq [packet packets]
      (enqueue! out-queue (dispatch packet)))))

(defn- write-out-queue [out-queue output-stream]
  (when-let [packets (keep identity (seq @out-queue))]
    (reset! out-queue (clojure.lang.PersistentQueue/EMPTY))
    (packet/write output-stream packets)))

(defn client
  "Opens a tcp socket and reads packets from input stream to in-queue,
  and writes packets from out-queue to output stream."
  ([]
   (client nil))
  ([{:keys [host port sleep-interval]}]
   (let [{:keys [in-queue out-queue] :as queues} QUEUES
         running (atom true)]
     (reset! out-queue (clojure.lang.PersistentQueue/EMPTY))
     (reset! in-queue (clojure.lang.PersistentQueue/EMPTY))
     (enqueue! out-queue (packets/insim-init))
     (future
       (with-open [socket (Socket. (or host "127.0.0.1") (or port 29999))
                   output-stream (clojure.java.io/output-stream socket)
                   input-stream (clojure.java.io/input-stream socket)]
         (while @running
           (read-in-queue input-stream in-queue)
           (dispatch-in-queue queues)
           (write-out-queue out-queue output-stream)
           (Thread/sleep (or sleep-interval 1500)))))
     running)))

(comment
  (enqueue! [(packets/mst "Hallo")
             (packets/mst "...")
             (packets/mst "Wereld")])

  (enqueue! (packets/close))

  (enqueue! (packets/request-npl))

  @connections
  @players

  (def lfs-client (client))
  (reset! lfs-client false)

  (remove-all-methods dispatch)
  )
