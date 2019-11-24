(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.codecs :as codecs]
            [clj-insim.packets :as packets]
            [marshal.core :as m])
  (:import [java.net Socket]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Parsers

(defn- parse-header [[size t request-info subtype]]
  (let [type (enums/isp t)
        m {:size size
           :type type
           :request-info request-info
           :subtype subtype}]
    (condp = type
      :tiny (update m :subtype enums/tiny)
      :small (update m :subtype enums/small)
      :ttc (update m :subtype enums/ttc)
      m)))

(defn- unparse-header [{:keys [type subtype] :as m}]
  ((juxt :size :type :request-info :subtype)
   (cond-> m
     (= type :tiny) (update :subtype enums/tiny)
     (= type :small) (update :subtype enums/small)
     (= type :ttc) (update :subtype enums/ttc)
     true (update :type enums/isp))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Reading & writing

(defn read-packet [input-stream]
  (let [{:keys [size] :as header} (parse-header (m/read input-stream codecs/header))
        body (when (> size 4)
               (m/read input-stream (codecs/body header)))]
    {:header header :body body}))

(defn write-packets [output-stream packets]
  (if (not (seq packets))
    (.flush output-stream)
    (recur
     (let [{:keys [header body]} (first packets)]
       (doto output-stream
         (m/write codecs/header (unparse-header header))
         (m/write (codecs/body header) body))) 
     (rest packets))))

(defn- dispatch [packet {:keys [out-queue]}]
  (swap! out-queue conj (packets/tiny))
  (println "Maintained connection...."))

(defn- process
  "Pops packets from in-queue and dispatches them."
  [running {:keys [in-queue out-queue] :as queues}]
  (future
    (while @running
      (while (seq @in-queue)
        (let [packet (peek @in-queue)]
          (swap! in-queue pop)
          (dispatch packet queues)))
      (Thread/sleep 500))))

(defn- connect
  "Opens a tcp socket and reads packets from input stream to in-queue,
  and writes packets from out-queue to output stream."
  [running {:keys [in-queue out-queue] :as queues}]
  (future
    (with-open [socket (Socket. "127.0.0.1" 29999)
                output-stream (clojure.java.io/output-stream socket)
                input-stream (clojure.java.io/input-stream socket)]
      (while @running
        (while (pos? (.available input-stream))
          (let [p (read-packet input-stream)]
            (swap! in-queue conj p)))
        (when-let [packets (seq @out-queue)]
          (reset! out-queue (clojure.lang.PersistentQueue/EMPTY))
          (write-packets output-stream packets))
        (Thread/sleep 1000)))))

(defn client [{:keys [out-queue] :as queues}]
  (swap! out-queue conj (packets/insim-init))
  (let [running (atom true)]
    (connect running queues)
    (process running queues)
    running))

(comment
  (seq @(:out-queue queues))

  (swap! (:out-queue queues) conj (packets/mst "Hallo!"))

  (swap! (:out-queue queues) concat [(packets/mst "Hallo") (packets/mst "...") (packets/mst "Wereld")])

  (def queues {:in-queue (atom (clojure.lang.PersistentQueue/EMPTY))
               :out-queue (atom (clojure.lang.PersistentQueue/EMPTY))})

  (def lfs-client (client queues))
  (reset! lfs-client false)

  (def processor (process queues))
  (reset! processor false)
)
