(ns clj-insim.client
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

(defn- maintain-connection-packet?
  "Returns a truethy value when a TINY/NONE packet is passed in as argument."
  [{:header/keys [type data]}]
  (and (#{:tiny} type) (#{:none} data)))

(defn- print-verbose [packet]
  (when @VERBOSE
    (newline)
    (println (str "IS_" (-> (:header/type packet) name str/upper-case) " packet!"))
    (println (str packet))))

(defn- dispatch
  "Dispatch is the entrypoint for automatic responses to certain packets, like
   the maintain connection concern."
  [{:keys [to-lfs]} packet]
  (when (maintain-connection-packet? packet)
    (a/>!! to-lfs (packets/tiny)))
  (print-verbose packet))

(defn- close-fn [{:keys [running? from-lfs to-lfs input-stream output-stream socket]}]
  (when @running?
    (a/>!! to-lfs (packets/tiny {:data :close}))
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
           running? (atom true)]
       (a/go
         (a/>!! to-lfs isi)
         (while @running?
           (let [packet (a/<! to-lfs)]
             (wrap-try-catch write/instruction output-stream packet))))
       (a/go
         (while @running?
           (when-let [packet (wrap-try-catch read/packet input-stream)]
             (dispatch {:to-lfs to-lfs} packet)
             (a/>! from-lfs packet))))
       (println "clj-insim: client started")
       {:from-lfs from-lfs
        :to-lfs to-lfs
        :stop (fn []
                (close-fn
                 {:from-lfs from-lfs
                  :input-stream input-stream
                  :output-stream output-stream
                  :running? running?
                  :socket socket
                  :to-lfs to-lfs}))}))))

(defn stop [{:keys [stop]}]
  (stop))

(comment
  (def lfs-client (start))
  (stop lfs-client)

  ;; In order to set verbose logging (log all incoming packets)
  (reset! VERBOSE true)

  ;; To send a packet to lfs
  (a/>!! (:to-lfs lfs-client) (packets/msl {:sound :error}))

  @ERROR_LOG
)
