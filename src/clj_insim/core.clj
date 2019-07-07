(ns clj-insim.core
  (:require [clj-insim.decoders :refer [decoders]]
            [clj-insim.encoders :refer [encoders]]
            [clj-insim.enums :as enums]
            [clojure.java.io :as io]
            [org.clojars.smee.binary.core :as sb])
  (:import [java.net Socket]))

(defn- enqueue! [queue packet]
  (swap! queue conj packet))

(defn- pop! [queue]
  (when-let [packet (peek @queue)]
    (swap! queue pop)
    packet))

(defn client [running {:keys [debug]}]
  (let [{:keys [in-queue out-queue] :as atoms}
        {:in-queue (atom (clojure.lang.PersistentQueue/EMPTY))
         :out-queue (atom (clojure.lang.PersistentQueue/EMPTY))}]
    (future
      (with-open [socket (Socket. "127.0.0.1" 29999)
                  input-stream (io/input-stream socket)
                  output-stream (io/output-stream socket)
                  _ (sb/encode (:isi encoders) output-stream (packets/isi))
                  _ (.flush output-stream)]
        (while @running
          (cond
            (pos? (.available input-stream))
            (let [header (sb/decode (sb/ordered-map :size :ubyte :type (sb/enum :ubyte enums/ISP) :reqi :ubyte) input-stream)
                  packet (if-let [decoder (get decoders (:type header))]
                           (merge header (sb/decode decoder input-stream))
                           (assoc header :raw (sb/decode (sb/repeated :ubyte :length (- (:size header) 3)) input-stream)))]
              (when debug
                (newline)
                (println "*** INCOMING PACKET ***")
                (println packet))
              (enqueue! in-queue packet))

            (peek @out-queue)
            (let [packet (pop! out-queue)]
              (when-let [encoder (get encoders (:type packet))]
                (when debug
                  (newline)
                  (println "### OUTGOING PACKET ###")
                  (println packet))
                (sb/encode encoder output-stream packet)
                (.flush output-stream)))

            :else
            (Thread/sleep 20)))))
    atoms))

(defn- handle [running in-queue handler out-queue]
  (future
    (while @running
      (if-let [packet (pop! in-queue)]
        (handler packet out-queue)
        (Thread/sleep 20)))))

(defmulti dispatch :type)
(defmethod dispatch :default [_ _] nil)

(defmethod dispatch :tiny [{:keys [sub-type]} queue]
  (when (= sub-type :none)
    (enqueue! queue (tiny :none))
    (enqueue! queue (msl "Maintained connection..."))))

(defmethod dispatch :ver [packet queue]
  (enqueue! queue (msl "Hello, World!")))

(defn start
  ([handler]
   (start handler {}))
  ([handler {:keys [debug] :as options}]
   (let [running (atom true)
         {:keys [in-queue out-queue] :as lfs-client} (client running options)
         routine (handle running in-queue handler out-queue)]
     running)))

(comment
  (def lfs-client (start dispatch {:debug true}))
  (reset! lfs-client false)
)
