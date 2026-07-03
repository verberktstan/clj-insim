(ns clj-insim.client
  "Provides functionality to create a InSim client. You'll want to use `start` to
   start a client and call `(stop client)` to stop it."
  (:require [clj-insim.channel :as channel]
            [clj-insim.connection :as connection]
            [clj-insim.logging :as logging]
            [clj-insim.models.packet :as packet]
            [clj-insim.packets :as packets]
            [clj-insim.read :as read]
            [clj-insim.write :as write]
            [clojure.core.async :as a]
            [clojure.java.io :as io]))

(defn >!!
  "(Blocking) put packet on the channel for sending to LFS."
  [client packet]
  (channel/>!! client packet))

(defn <!!
  "(Blocking) take packet from the channel for receiving from LFS."
  [client packet]
  (channel/<!! client packet))

(defn >!
  "(Async) put packet on the channel for sending to LFS."
  [client packet]
  (channel/>! client packet))

(defn <!
  "(Ascync) take packet from the channel for receiving from LFS."
  [client packet]
  (channel/<! client packet))

(defn- dispatch
  "Dispatch is the entrypoint for automatic responses to certain packets, like
   the maintain connection concern."
  [client packet]
  (when (packet/maintain-connection? packet)
    (channel/>!! client (packets/tiny)))
  (logging/print-verbose packet))

(defn- close-fn [{:keys [running? from-lfs to-lfs input-stream output-stream socket] :as client}]
  (when @running?
    (channel/>!! client (packets/tiny {:data :close}))
    (channel/close! client)
    (Thread/sleep 10) ;; TODO, fix this!
    (reset! running? false)
    (connection/close-socket! socket input-stream output-stream)
    (println "clj-insim: client stopped")))

(defn start
  "Opens a socket, streams and async channels to connect with Live For Speed via InSim.
   Returns a map containing `::from-lfs-chan`, `::to-lfs-chan` & `::close!`
   `(a/>!! to-lfs-chan packet)` makes the client send the packet to lfs.
   `(a/<!! from-lfs-chan)` returns a packet from LFS if available. Preferrably do
   this in a go block / loop. Evaluate `::close!` to stop and close the client."
  ([]
   (start nil))
  ([{:keys [host port isi] :or {host "127.0.0.1" port 29999 isi (packets/isi)} :as options}]
   (when-let [socket (connection/make-socket host port)]
     (let [input-stream (io/input-stream socket)
           output-stream (io/output-stream socket)
           from-lfs (a/chan (a/sliding-buffer 10))
           to-lfs (a/chan (a/sliding-buffer 10))
           running? (atom true)
           new-byte-size? (> (:body/insim-version isi) 8)]
       (println "clj-insim: using INSIM_VERSION:" (:body/insim-version isi))
       (a/go
         (a/>!! to-lfs isi)
         (while @running?
           (let [packet (a/<! to-lfs)]
             (logging/wrap-try-catch (write/instruction new-byte-size?) output-stream packet))))
       (a/go
         (while @running?
           (when-let [packet (logging/wrap-try-catch (read/packet new-byte-size?) input-stream)]
             (dispatch {:to-lfs to-lfs} packet)
             (a/>! from-lfs packet))))
       (println "clj-insim: client started")
       {:from-lfs from-lfs
        :to-lfs to-lfs
        :running? running?
        :stop (fn []
                (close-fn
                 {:from-lfs from-lfs
                  :input-stream input-stream
                  :output-stream output-stream
                  :running? running?
                  :socket socket
                  :to-lfs to-lfs}))}))))

(defn running? [client]
  @(:running? client))

(defn go
  "Start a async go-loop that calls `dispatch` on every incoming packet.
   The dispatch function should accept the client as first argument and the
   incoming packet as second argument."
  [{:keys [from-lfs] :as client} dispatch]
  (a/go
    (while (running? client)
      (when-let [packet (a/<! from-lfs)]
        (dispatch client packet)))))

(defn stop
  "When passed a running client (map) as argument, stops the client, in/output
   streams and the socket."
  [{:keys [stop]}]
  (stop))

(comment
  (def lfs-client (start))
  (def lfs-client (start {:host "192.168.2.11" :port 29999}))
  (stop lfs-client)

  ;; In order to set verbose logging (log all incoming packets)
  (reset! logging/VERBOSE true)

  ;; To send a packet to lfs
  (>!! lfs-client (packets/msl {:sound :error}))

  @logging/ERROR_LOG)
