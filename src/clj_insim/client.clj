(ns clj-insim.client
  (:require [clj-insim.models.packet :as packet]
            [clj-insim.packets :as packets]
            [clj-insim.read :as read]
            [clj-insim.write :as write]
            [clojure.core.async :as a]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component])
  (:import [java.net Socket]))

(def ERROR_LOG (atom nil))
(defonce ERRORS (atom true))
(defonce VERBOSE (atom false))

(defonce to-lfs (atom nil))
(defonce from-lfs (atom nil))

(defonce ^:private running? (atom false))

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
  [packet]
  (when (maintain-connection-packet? packet)
    (a/>!! @to-lfs (packets/tiny)))
  (print-verbose packet))

(defn- close-fn [{:keys [input-stream output-stream socket]}]
  (when @running?
    (a/>!! @to-lfs (packets/tiny {:data :close}))
    (reset! running? false)
    (when @from-lfs
      (a/close! @from-lfs)
      (reset! from-lfs nil))
    (when @to-lfs
      (a/close! @to-lfs)
      (reset! to-lfs nil))
    (Thread/sleep 10) ;; TODO, fix this!
    (.close input-stream)
    (.close output-stream)
    (.close socket)
    (println "clj-insim: client stopped")))

(defn- make-socket [host port]
  (try
    (Socket. host port)
    (catch java.net.ConnectException e
      (println (.getMessage e) "\nMake sure you've run `/insim 29999` in LFS."))))

(defn start-client
  "Opens a socket, streams and async channels to connect with Live For Speed via InSim.
   Returns a map containing `::from-lfs-chan`, `::to-lfs-chan` & `::close!`
   `(a/>!! to-lfs-chan packet)` makes the client send the packet to lfs.
   `(a/<!! from-lfs-chan)` returns a packet from LFS if available. Preferrably do
   this in a go block / loop. Evaluate `::close!` to stop and close the client."
  ([]
   (start-client nil))
  ([{:keys [host port isi] :or {host "127.0.0.1" port 29999 isi (packets/isi)}}]
   (when-let [socket (make-socket host port)]
     (let [input-stream (io/input-stream socket)
           output-stream (io/output-stream socket)]
       (a/go
         (a/>!! @to-lfs isi)
         (while @running?
           (try
             (let [packet (a/<! @to-lfs)]
               (write/instruction output-stream packet))
             (catch Throwable t
               (when @ERRORS
                 (swap! ERROR_LOG conj (Throwable->map t))
                 (println "clj-insim write error:" (.getMessage t)))))))
       (a/go
         (while @running?
           (when-let [packet (try
                               (read/packet input-stream)
                               (catch Throwable t
                                 (when @ERRORS
                                   (swap! ERROR_LOG conj (Throwable->map t))
                                   (println "clj-insim read error:" (.getMessage t)))))]
             (dispatch packet)
             (a/>! @from-lfs packet))))
       (println "clj-insim: client started")
       (fn []
         (close-fn
          {:input-stream input-stream
           :output-stream output-stream
           :socket socket}))))))

(defn- stop-client [client]
  (when client (client)))

(defrecord Client []
  component/Lifecycle
  (start [this]
    (reset! from-lfs (a/chan (a/sliding-buffer 10)))
    (reset! to-lfs (a/chan (a/sliding-buffer 10)))
    (reset! running? true)
    (assoc this :client (start-client)))
  (stop [this]
    (stop-client (:client this))
    (dissoc this :client)))

(defn create-client []
  (Client.))

(comment
  #_(.start (create-client))


  ;; Start a client
  (def lfs-client (start-client))
  ;; Print the first packet that we receive from LFS
  (a/go (println (a/<! (::from-lfs-chan lfs-client))))
  ;; Stop the client
  (stop! lfs-client)

  (reset! VERBOSE true)

  @ERROR_LOG

  (let [packet (packets/plc {:ucid 0 :cars #{"FZR" "FBM"}})]
    (a/>!! (::to-lfs-chan lfs-client) packet))

  (let [packet (packets/mtc {:text "Hello world!"})]
    (a/>!! (::to-lfs-chan lfs-client) packet))
)
