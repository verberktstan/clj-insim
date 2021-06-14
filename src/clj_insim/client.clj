(ns clj-insim.client
  (:require [clj-insim.codecs :as codecs]
            [clj-insim.packets :as packets]
            [clj-insim.parse :refer [unparse]]
            [clj-insim.read :as read]
            [clojure.core.async :as a]
            [clojure.java.io :as io]
            [marshal.core :as m])
  (:import [java.net Socket]))

(defn- write-packet [output-stream {:header/keys [size type] :as packet}]
  (let [unparsed (unparse packet)]
    (m/write output-stream codecs/header unparsed)
    (when (> size 4)
      (m/write output-stream (get codecs/body type) unparsed))
    (.flush output-stream)))

(defn- read-packet [input-stream]
  (when-let [header (read/header input-stream)]
    (merge header (read/body input-stream header))))

(defn start
  "Opens a socket, streams and async channels to connect with Live For Sspeed via InSim."
  []
  (let [running? (atom true)
        to-lfs-chan (a/chan (a/sliding-buffer 2))
        from-lfs-chan (a/chan (a/sliding-buffer 2))
        socket (Socket. "127.0.0.1" 29999) ;; TODO: Pass in host/port as arguments
        output-stream (io/output-stream socket)
        input-stream (io/input-stream socket)
        close! (fn []
                 (.close input-stream)
                 (.close output-stream)
                 (.close socket))]
    (a/go
      (a/>! to-lfs-chan (packets/isi)) ;; TODO: Pass in the ISI packet as argument
      (while @running?
        (let [packet (a/<! to-lfs-chan)]
          (write-packet output-stream packet))))
    (a/go
      (while @running?
        (when-let [packet (read-packet input-stream)]
          (a/>! from-lfs-chan packet))))
    {::running? running?
     ::to-lfs-chan to-lfs-chan
     ::from-lfs-chan from-lfs-chan
     ::close! close!}))

(defn stop! [{::keys [close! running?]}]
  (reset! running? false)
  (close!))

(comment
  (def lfs-client (start))
  (a/go (println (a/<! (::from-lfs-chan lfs-client))))
  (stop! lfs-client)
)
