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
  (let [unparsed (unparse packet)
        body-codec (get codecs/body type #(m/struct :body/unkown (m/ascii-string (- size 4))))]
    (m/write output-stream codecs/header unparsed)
    (when (> size 4)
      (m/write output-stream (body-codec packet) unparsed))
    (.flush output-stream)))

(defn- read-packet [input-stream]
  (when-let [header (read/header input-stream)]
    (merge header (read/body input-stream header))))

(defn- maintain-connection-packet? [{:header/keys [type data]}]
  (and (#{:tiny} type) (#{:none} data)))

(defn- make-channels []
  {::to-lfs-chan   (a/chan (a/sliding-buffer 2))
   ::from-lfs-chan (a/chan (a/sliding-buffer 2))})

(defn- make-streams [socket]
  {::output-stream (io/output-stream socket)
   ::input-stream  (io/input-stream socket)})

(defn- dispatch
  "Dispatch is the entrypoint for automatic responses to certain packets, like
   the maintain connection concern."
  [{::keys [to-lfs-chan]} packet]
  (when (maintain-connection-packet? packet)
    (a/>!! to-lfs-chan (packets/tiny))))

(defn start
  "Opens a socket, streams and async channels to connect with Live For Sspeed via InSim."
  ([]
   (start nil))
  ([{:keys [host port]
      :or {host "127.0.0.1"
           port 29999}}]
   (let [running? (atom true)
         {::keys [to-lfs-chan from-lfs-chan] :as channels} (make-channels)
         socket (Socket. host port)
         {::keys [input-stream output-stream] :as streams} (make-streams socket)
         close! (fn [] (.close input-stream) (.close output-stream) (.close socket))]
     (a/go
       (a/>! to-lfs-chan (packets/isi)) ;; TODO: Pass in the ISI packet as argument
       (while @running?
         (let [packet (a/<! to-lfs-chan)]
           (write-packet output-stream packet))))
     (a/go
       (while @running?
         (when-let [packet (read-packet input-stream)]
           (dispatch channels packet)
           (a/>! from-lfs-chan packet))))
     (merge channels {::running? running? ::close! close!}))))

(defn stop! [{::keys [close! running?]}]
  (reset! running? false)
  (close!))

(comment
  (def lfs-client (start))
  (a/go (println (a/<! (::from-lfs-chan lfs-client))))
  (stop! lfs-client)
)
