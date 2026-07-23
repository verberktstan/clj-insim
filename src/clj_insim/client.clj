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

(defonce ^:private game-state (atom nil))
(defonce ^:private players (atom {}))       ;; player-id -> full packet
(defonce ^:private connections (atom {}))   ;; ucid -> full packet

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

(defn get-game-state
  "Returns the latest IS_STA packet received from LFS, or nil if not yet received.
   Contains game state info like track, wind, player counts, race status, etc."
  []
  @game-state)

(defn get-players
  "Returns map of all players: player-id -> full IS_NPL packet"
  []
  @players)

(defn get-player
  "Returns IS_NPL packet for player by player-id, or nil"
  [player-id]
  (get @players player-id))

(defn get-connections
  "Returns map of all connections: ucid -> full IS_NCN packet"
  []
  @connections)

(defn get-connection
  "Returns IS_NCN packet for connection by ucid, or nil"
  [ucid]
  (get @connections ucid))

(defn- format-game-state
  "Converts the raw IS_STA packet to a user-friendly map with :state/ namespaced keys.
   race-in-progress is already parsed to :no-race, :race, or :qualifying.
   Returns nil if packet is nil."
  [raw-packet]
  (when raw-packet
    (let [{:body/keys [replay-speed flags in-game-cam view-player-id
                       num-players num-connections num-finished race-in-progress
                       qualify-minutes race-laps track wind]} raw-packet]
      #:state{:race-state race-in-progress
              :flags flags
              :player-count num-players
              :connection-count num-connections
              :finished-count num-finished
              :session-duration-minutes qualify-minutes
              :total-laps race-laps
              :track track
              ;; :weather weather ; NOTE: No documented enum!
              :wind wind
              :replay-speed replay-speed
              :camera-mode in-game-cam
              :viewed-player-id (when (pos? view-player-id) view-player-id)})))

(defn- format-connection
  "Converts the raw IS_NCN packet to a user-friendly map with :player/ namespaced keys.
   admin is already parsed to :admin or nil; coerced to boolean.
   Returns nil if packet is nil."
  [raw-packet]
  (when raw-packet
    (let [{:body/keys [user-name player-name admin flags]} raw-packet]
      #:player{:user-name user-name
               :name player-name
               :admin? (boolean admin)
               :flags flags})))

(defn- rename-player-by-ucid [players ucid player-name]
  (reduce-kv (fn [players pid player]
               (cond-> players
                 (= ucid (get-in player [:header/ucid]))
                 (assoc-in [pid :body/player-name] player-name)))
             players
             players))

(defn- dispatch
  "Dispatch is the entrypoint for automatic responses to certain packets, like
   the maintain connection concern."
  [{:keys [to-lfs] :as client} {:header/keys [type ucid player-id] :body/keys [player-name new-ucid] :as packet}]
  (case type
    :sta (reset! game-state (format-game-state packet))
    :ncn (swap! connections assoc ucid (format-connection packet))
    :npl (swap! players assoc player-id packet)
    :pll (swap! players dissoc player-id)
    :cnl (swap! connections dissoc ucid)
    :cpr (do
           (swap! connections assoc-in [ucid :player/name] player-name)
           (swap! players rename-player-by-ucid ucid player-name))
    :toc (when (contains? @players player-id)
           (swap! players assoc-in [player-id :header/ucid] new-ucid))
    :ver (when to-lfs
           (run! (partial channel/>!! client)
                 [(packets/tiny {:request-info 1 :data :sst})
                  (packets/tiny {:request-info 1 :data :npl})
                  (packets/tiny {:request-info 1 :data :ncn})]))
    nil)
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
    (logging/stop-packet-logging!)
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
   (println "Starting clj-insim client on host:" host "port:" port)
   (when-let [socket (connection/make-socket host port)]
     (logging/init-packet-logging!)
     (let [input-stream (io/input-stream socket)
           output-stream (io/output-stream socket)
           from-lfs (a/chan (a/sliding-buffer 10))
           to-lfs (a/chan (a/sliding-buffer 10))
           running? (atom true)
           new-byte-size? (> (:body/insim-version isi) 8)]
       (println "clj-insim: using INSIM_VERSION:" (:body/insim-version isi))
       (a/go
         (a/>!! to-lfs isi)
         (logging/log-outgoing isi)
         (while @running?
           (let [packet (a/<! to-lfs)
                 write! (write/instruction new-byte-size?)]
             (when packet (logging/log-outgoing packet))
             (logging/wrap-try-catch write! output-stream packet)
             (loop []
               (when-let [queued (a/poll! to-lfs)]
                 (logging/log-outgoing queued)
                 (logging/wrap-try-catch write! output-stream queued)
                 (recur)))
             (write/flush! output-stream))))
       (a/go
         (while @running?
           (when-let [packet (logging/wrap-try-catch (read/packet new-byte-size?) input-stream)]
             (logging/log-raw-bytes
              (byte-array [(-> packet :header/size byte)
                           (-> packet :header/size (bit-shift-right 8) byte)
                           (-> packet :header/type name first byte)
                           (byte 0)]))
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

  (get-game-state)
  (get-players)
  (get-connections)
  ;; In order to set verbose logging (log all incoming packets)
  (reset! logging/VERBOSE true)

  ;; To send a packet to lfs
  (>!! lfs-client (packets/msl {:sound :error}))

  @logging/ERROR_LOG)
