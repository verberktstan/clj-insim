(ns clj-insim.core
  (:require [clj-insim.parse :as parse]
            [clj-insim.codecs :as codecs]
            [clj-insim.packets :as packets]
            [clj-insim.models.packet :as packet]
            [clj-insim.queues :refer [enqueue!] :as queues]
            [clojure.java.io :as io]
            [marshal.core :as m])
  (:import [java.net Socket]))

(defonce version (atom nil))
(defonce state (atom nil))

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
     (queues/reset!)
     (enqueue! (packets/insim-init))
     (future
       (with-open [socket (Socket. (or host "127.0.0.1") (or port 29999))
                   output-stream (io/output-stream socket)
                   input-stream (io/input-stream socket)]
         (while @running
           (queues/read! input-stream)
           (queues/dispatch! dispatch-fn)
           (queues/write! output-stream)
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
