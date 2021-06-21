(ns clj-insim.client
  (:require [clj-insim.packets :as packets]
            [clj-insim.read :as read]
            [clj-insim.write :as write]
            [clojure.core.async :as a]
            [clojure.java.io :as io])
  (:import [java.net Socket]))

(defn- maintain-connection-packet?
  "Returns a truethy value when a TINY/NONE packet is passed in as argument."
  [{:header/keys [type data]}]
  (and (#{:tiny} type) (#{:none} data)))

(defn- make-channels
  "Returns a map with the channels that convey packets to/from LFS."
  []
  {::to-lfs-chan   (a/chan (a/sliding-buffer 10))
   ::from-lfs-chan (a/chan (a/sliding-buffer 10))})

(defn- make-streams
  "Returns a map with streams necessary for communication with LFS."
  [socket]
  {::output-stream (io/output-stream socket)
   ::input-stream  (io/input-stream socket)})

(defn- dispatch
  "Dispatch is the entrypoint for automatic responses to certain packets, like
   the maintain connection concern."
  [{::keys [to-lfs-chan]} packet]
  (when (maintain-connection-packet? packet)
    (a/>!! to-lfs-chan (packets/tiny))))

(defn start
  "Opens a socket, streams and async channels to connect with Live For Speed via InSim.
   Returns a map containing `::from-lfs-chan`, `::to-lfs-chan` & `::close!`
   `(a/>!! to-lfs-chan packet)` makes the client send the packet to lfs.
   `(a/<!! from-lfs-chan)` returns a packet from LFS if available. Preferrably do
   this in a go block / loop. Evaluate `::close!` to stop and close the client."
  ([]
   (start nil))
  ([{:keys [host port isi] :or {host "127.0.0.1" port 29999 isi (packets/isi)}}]
   (let [running? (atom true)
         {::keys [from-lfs-chan to-lfs-chan] :as channels} (make-channels)
         socket (Socket. host port)
         {::keys [input-stream output-stream] :as streams} (make-streams socket)
         close! (fn []
                  (a/>!! to-lfs-chan (packets/tiny {:data :close}))
                  (reset! running? false)
                  (a/close! from-lfs-chan)
                  (a/close! to-lfs-chan)
                  (Thread/sleep 50)
                  (.close input-stream)
                  (.close output-stream)
                  (.close socket)
                  (println "clj-insim: client stopped"))]
     (a/go
       (a/>! to-lfs-chan isi)
       (while @running?
         (let [packet (a/<! to-lfs-chan)]
           (write/packet! output-stream packet))))
     (a/go
       (while @running?
         (when-let [packet (read/packet input-stream)]
           (dispatch channels packet)
           (a/>! from-lfs-chan packet))))
     (println "clj-insim: client started")
     (merge channels {::close! close!}))))

(defn stop! [{::keys [close!]}]
  (close!))

(comment
  ;; Start a client
  (def lfs-client (start))
  ;; Print the first packet that we receive from LFS
  (a/go (println (a/<! (::from-lfs-chan lfs-client))))
  ;; Stop the client
  (stop! lfs-client)

  (let [packet (packets/scc {:player-id 0 :in-game-cam :follow})]
    (a/>!! (::to-lfs-chan lfs-client) packet))
  
  (let [packet (packets/mtc {:text "Hello world!"})]
    (a/>!! (::to-lfs-chan lfs-client) packet))
)
