(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.codecs :as codecs]
            [clj-insim.packets :as packets]
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
     (swap! queue (cond (map? p) conj (seq p) concat) p))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Parsers

(defn parse-subtype [type subtype]
  ((get {:tiny enums/parse-tiny
         :small enums/parse-small
         :ttc enums/parse-ttc} type identity) subtype))

(defn- parse-header [[size t request-info subtype]]
  (let [type (enums/parse-isp t)]
    (update
     {:size size
      :type type
      :request-info request-info
      :subtype subtype}
     :subtype #(parse-subtype type %))))

(defn- unparse-header [{:keys [type subtype] :as m}]
  ((juxt :size :type :request-info :subtype)
   (update (cond-> m
             (= type :tiny) (update :subtype enums/unparse-tiny)
             (= type :small) (update :subtype enums/unparse-small)
             (= type :ttc) (update :subtype enums/unparse-ttc))
           :type enums/unparse-isp)))


(defmulti parse-body :type)
(defmethod parse-body :default [header body]
  body)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Reading & writing

(defn read-packet [input-stream]
  (let [{:keys [size] :as header} (parse-header (m/read input-stream codecs/header))]
    {:header header
     :body (when (> size 4)
             (parse-body header (m/read input-stream (codecs/body header))))}))

(defn write-packets [output-stream packets]
  (if (not (seq packets))
    (.flush output-stream)
    (recur
     (let [{:keys [header body]} (first packets)]
       (doto output-stream
         (m/write codecs/header (unparse-header header))
         (m/write (codecs/body header) body))) 
     (rest packets))))

(defmulti dispatch #(get-in % [:header :type]))

(defmethod dispatch :default [packet]
  (when DEBUG
    (newline)
    (println "Default dispatch: " packet)))

(defmethod dispatch :ver [packet]
  (enqueue! (packets/mst "Dispatching a VER packet")))

(defmethod dispatch :tiny [packet]
  (enqueue! [(packets/mst "Maintaining..") (packets/mst "..connection!")])
  (enqueue! (packets/tiny)))

(defn client
  "Opens a tcp socket and reads packets from input stream to in-queue,
  and writes packets from out-queue to output stream."
  ([]
   (client nil))
  ([{:keys [host port sleep-interval]}]
   (let [{:keys [in-queue out-queue] :as queues} QUEUES
         running (atom true)]
     (reset! out-queue (clojure.lang.PersistentQueue/EMPTY))
     (enqueue! (packets/insim-init))
     (future
       (with-open [socket (Socket. (or host "127.0.0.1") (or port 29999))
                   output-stream (clojure.java.io/output-stream socket)
                   input-stream (clojure.java.io/input-stream socket)]
         (while @running
           (while (pos? (.available input-stream))
             (let [p (read-packet input-stream)]
               (enqueue! in-queue p)))

           (while (seq @in-queue)
             (let [packet (peek @in-queue)]
               (swap! in-queue pop)
               (dispatch packet)))

           (when-let [packets (seq @out-queue)]
             (reset! out-queue (clojure.lang.PersistentQueue/EMPTY))
             (write-packets output-stream packets))
           (Thread/sleep (or sleep-interval 500)))))
     running)))

(comment
  (enqueue! (packets/mst "Hallo!"))

  (enqueue! [(packets/mst "Hallo") (packets/mst "...") (packets/mst "Wereld")])

  (def lfs-client (client))
  (reset! lfs-client false)
  )
