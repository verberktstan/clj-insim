(ns clj-insim.client
  (:require [clj-insim.codecs :as codecs]
            [clj-insim.packets :as packets]
            [clj-insim.parse :as parse]
            [clj-insim.read :as read]
            [clojure.core.async :as a]
            [clojure.java.io :as io]
            [marshal.core :as m])
  (:import [java.net Socket]))

(defn- write-packet [output-stream {:header/keys [size type] :as packet}]
  (let [instruction (parse/instruction packet)
        body-codec (get codecs/body type #(m/struct :body/unkown (m/ascii-string (- size 4))))]
    (m/write output-stream codecs/header instruction)
    (when (> size 4)
      (m/write output-stream (body-codec packet) instruction))
    (.flush output-stream)))

(defn- read-packet [input-stream]
  (when-let [header (read/header input-stream)]
    (merge header (read/body input-stream header))))

(defn- maintain-connection-packet? [{:header/keys [type data]}]
  (and (#{:tiny} type) (#{:none} data)))

(defn- make-channels []
  {::to-lfs-chan   (a/chan (a/sliding-buffer 10))
   ::from-lfs-chan (a/chan (a/sliding-buffer 10))})

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
  "Opens a socket, streams and async channels to connect with Live For Sspeed via InSim.
   Returns a map containing `::from-lfs-chan`, `::to-lfs-chan` & `::close!`
   `(a/>!! to-lfs-chan packet)` makes the client send the packet to lfs.
   `(a/<!! from-lfs-chan)` returns a packet from lfs if available. Preferrably do
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
           (write-packet output-stream packet))))
     (a/go
       (while @running?
         (when-let [packet (read-packet input-stream)]
           (dispatch channels packet)
           (a/>! from-lfs-chan packet))))
     (println "clj-insim: client started")
     (merge channels {::close! close!}))))

(defn stop! [{::keys [close!]}]
  (close!))

(comment
  (def lfs-client (start))
  (a/go (println (a/<! (::from-lfs-chan lfs-client))))
  (stop! lfs-client)

  (let [packet (packets/scc {:player-id 0 :in-game-cam :follow})]
    (a/>!! (::to-lfs-chan lfs-client) packet))
  
  (let [packet (packets/mtc {:text "Hello world!"})]
    (a/>!! (::to-lfs-chan lfs-client) packet))
)
