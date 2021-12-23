(ns clj-insim.client
  "Provides functionality to create a InSim client. You'll want to use `start` to
   start a client and call `(stop client)` to stop it."
  (:require [clj-insim.models.packet :as packet]
            [clj-insim.packets :as packets]
            [clj-insim.read :as read]
            [clj-insim.write :as write]
            [clojure.core.async :as a]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.net Socket]))

(def ERROR_LOG (atom nil))
(defonce ERRORS (atom true))
(defonce VERBOSE (atom false))

(defn >!!
  "(Blocking) put packet on the channel for sending to LFS."
  [client packet]
  (a/>!! (:to-lfs client) packet))

(defn <!!
  "(Blocking) take packet from the channel for receiving from LFS."
  [client packet]
  (a/<!! (:from-lfs client)))

(defn >!
  "(Async) put packet on the channel for sending to LFS."
  [client packet]
  (a/go (a/>! (:to-lfs client) packet)))

(defn <!
  "(Ascync) take packet from the channel for receiving from LFS."
  [client packet]
  (a/go (a/<! (:from-lfs client))))

(defn- print-verbose [packet]
  (when @VERBOSE
    (newline)
    (println (str "IS_" (-> (:header/type packet) name str/upper-case) " packet!"))
    (println (str packet))))

(defn- dispatch
  "Dispatch is the entrypoint for automatic responses to certain packets, like
   the maintain connection concern."
  [client packet]
  (when (packet/maintain-connection? packet)
    (>!! client (packets/tiny)))
  (print-verbose packet))

(defn- close-fn [{:keys [running? from-lfs to-lfs input-stream output-stream socket] :as client}]
  (when @running?
    (>!! client (packets/tiny {:data :close}))
    (a/close! from-lfs)
    (a/close! to-lfs)
    (Thread/sleep 10) ;; TODO, fix this!
    (reset! running? false)
    (.close input-stream)
    (.close output-stream)
    (.close socket)
    (println "clj-insim: client stopped")))

(defn- make-socket [host port]
  (try
    (Socket. host port)
    (catch java.net.ConnectException e
      (println (.getMessage e) (format "\nPlease run `/insim %d` in LFS." port)))))

(defn- log-throwable [t]
  (when @ERRORS
    (swap! ERROR_LOG conj (Throwable->map t))
    (println "clj-insim error:" (.getMessage t))))

(defn- wrap-try-catch [f & args]
  (try (apply f args) (catch Throwable t (log-throwable t))))

(defn start
  "Opens a socket, streams and async channels to connect with Live For Speed via InSim.
   Returns a map containing `::from-lfs-chan`, `::to-lfs-chan` & `::close!`
   `(a/>!! to-lfs-chan packet)` makes the client send the packet to lfs.
   `(a/<!! from-lfs-chan)` returns a packet from LFS if available. Preferrably do
   this in a go block / loop. Evaluate `::close!` to stop and close the client."
  ([]
   (start nil))
  ([{:keys [host port isi] :or {host "127.0.0.1" port 29999 isi (packets/isi)} :as options}]
   (when-let [socket (make-socket host port)]
     (let [input-stream (io/input-stream socket)
           output-stream (io/output-stream socket)
           from-lfs (a/chan (a/sliding-buffer 10))
           to-lfs (a/chan (a/sliding-buffer 10))
           running? (atom true)
           new-byte-size? (> (:body/insim-version isi) 8)]
       (println "clj-insim: using INSIM_VERSION: " (:body/insim-version isi))
       (a/go
         (a/>!! to-lfs isi)
         (while @running?
           (let [packet (a/<! to-lfs)]
             (wrap-try-catch (write/instruction new-byte-size?) output-stream packet))))
       (a/go
         (while @running?
           (when-let [packet (wrap-try-catch (read/packet new-byte-size?) input-stream)]
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
  (stop lfs-client)

  ;; In order to set verbose logging (log all incoming packets)
  (reset! VERBOSE true)

  ;; To send a packet to lfs
  (>!! lfs-client (packets/msl {:sound :error}))

  @ERROR_LOG
)
