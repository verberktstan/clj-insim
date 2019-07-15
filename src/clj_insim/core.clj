(ns clj-insim.core
  (:require [clj-insim.codecs :refer [header->body-codec]]
            [clj-insim.encoders :refer [encoders]]
            [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clojure.java.io :as io]
            [org.clojars.smee.binary.core :as sb])
  (:import [java.net Socket]))

(defn- enqueue! [queue packet]
  (swap! queue conj packet))

(defn- pop! [queue]
  (when-let [packet (peek @queue)]
    (swap! queue pop)
    packet))

(defn- rename-data-keys [m]
  (clojure.set/rename-keys m {:sound :data :sub-type :data}))

(def ^:private header-codec (sb/compile-codec
                             (sb/ordered-map :size :ubyte :type (sb/enum :ubyte enums/ISP) :reqi :ubyte :data :ubyte)
                             rename-data-keys
                             identity))
(def ^:private packet-codec (sb/header header-codec header->body-codec nil :keep-header? true))

(comment
  (let [baos (java.io.ByteArrayOutputStream.)
        _ (sb/encode packet-codec baos (packets/isi))]
    (seq (.toByteArray baos)))

  (let [baos (java.io.ByteArrayOutputStream.)
        _ (sb/encode packet-codec baos (packets/msl "Hello, world!"))]
    (seq (.toByteArray baos)))

  (let [baos (java.io.ByteArrayOutputStream.)
        _ (sb/encode (sb/padding (sb/c-string "UTF8") :length 16 :padding-byte 0) baos "abcdefghijklmab")]
    (seq (.toByteArray baos)))
)

(defn- print-debug [s packet]
  (newline)
  (println (str "DEBUG :: " s " :: DEBUG"))
  (prn packet))

(defn test-client [running {:keys [debug]}]
  (let [{:keys [in-queue out-queue] :as atoms} {:in-queue (atom (clojure.lang.PersistentQueue/EMPTY))
                                                :out-queue (atom (conj clojure.lang.PersistentQueue/EMPTY (packets/isi)))}]
    (future
      (with-open [socket (Socket. "127.0.0.1" 29999)
                  input-stream (io/input-stream socket)
                  output-stream (io/output-stream socket)]
        (while @running
          (cond
            (pos? (.available input-stream))
            (while (pos? (.available input-stream))
              (let [packet (try (sb/decode packet-codec input-stream)
                                (catch Exception e (doto (str "caught exception: " (.getMessage e)) println)))]
                (when debug (print-debug "Incoming packet" packet))
                (enqueue! in-queue packet)))

            (peek @out-queue)
            (while (peek @out-queue)
              (let [packet (pop! out-queue)]
                (when debug (print-debug "Outgoing packet" packet))
                (try (do (sb/encode packet-codec output-stream packet)
                         (.flush output-stream))
                     (catch Exception e (doto (str "Encode exception: " (.getMessage e)) println)))))

            :else
            (Thread/sleep 20)))))
    atoms))

(comment
  (def lfs-client (test-client running {:debug true}))
  (pop! (:in-queue lfs-client))
  (reset! (:running lfs-client) false)
  (enqueue! (:out-queue lfs-client) (packets/isi))
  (enqueue! (:out-queue lfs-client) (packets/msl "Test met de queues"))


  (let [baos (java.io.ByteArrayOutputStream.)
        _ (sb/encode packet-codec baos {:header {:size 8 :type :isi :reqi 2 :data 1}
                                        :body [0 1 2 3]})]
    (seq (.toByteArray baos)))

  (let [codec (sb/header (sb/ordered-map :size :ubyte :type :ubyte :reqi :ubyte :data :ubyte)
                         (fn header->body-codec [{:keys [size] :as x}] (println x)
                                                         (sb/repeated :ubyte :length (- size 4)))
                         nil
                         :keep-header? true)
        baos (java.io.ByteArrayOutputStream.)
        _ (sb/encode codec baos {:header {:size 8 :type 7 :reqi 6 :data 5}
                                 :body [4 3 2 1]})
        arr (.toByteArray baos)
        decoded (sb/decode codec (java.io.ByteArrayInputStream. (byte-array [8 7 6 5 4 3 2 1])))
        ]
    (seq arr))

)

(defn- handle [running in-queue handler out-queue]
  (future
    (while @running
      (if-let [packet (pop! in-queue)]
        (handler packet out-queue)
        (Thread/sleep 20)))))

(defmulti dispatch #(get-in % [:header :type]))
(defmethod dispatch :default [p out-queue]
  (prn "DISPATCHING >> " p))

(defmethod dispatch :tiny [p out-queue]
  (enqueue! out-queue (packets/tiny :none)))

(defn start
  ([handler]
   (start handler {}))
  ([handler {:keys [debug] :as options}]
   (let [running (atom true)
         {:keys [in-queue out-queue] :as lfs-client} (test-client running options)
         _ (handle running in-queue handler out-queue)]
     running)))

(comment
  (def lfs-client (start dispatch {:debug true}))
  (reset! lfs-client false)

  (def header-codec (sb/ordered-map :size :ubyte :type :ubyte :reqi :ubyte :data :ubyte))
  (defn header->body-codec [{:keys [type]}]
    (if (= type 2)
      (sb/ordered-map :one :ubyte :two :ubyte :three :ubyte :four :ubyte)
      (sb/blob :length 4)))
  (def body->header identity)

  (let [baos (java.io.ByteArrayOutputStream.)
        decoded (sb/decode (sb/header header-codec header->body-codec body->header :keep-header? true) (java.io.ByteArrayInputStream. (byte-array [4 2 1 0 1 2 3 4])))]
    decoded)
)
