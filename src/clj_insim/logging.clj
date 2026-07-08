(ns clj-insim.logging
  "Verbose packet logging and error tracking for the InSim client.

   Three registry-managed streams, each with its own buffer and file:
   - :raw (raw-bytes.txt): Raw TCP bytes before decoding
   - :parsed (parsed-incoming.edn): Decoded packets after parsing
   - :outgoing (outgoing.edn): Packets before encoding

   Buffers hold up to 100 entries. Writes triggered by: eager flush (buffer full),
   periodic auto-flush (every 5s), manual flush-all-buffers!, or shutdown.
   This trades latency for I/O efficiency while preventing data loss."
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(def ERROR_LOG (atom nil))
(defonce ERRORS (atom true))
(defonce VERBOSE (atom false))

(def ^:dynamic *packet-logging* false)
(def RAW_BYTES_BUFFER (atom []))
(def PARSED_INCOMING_BUFFER (atom []))
(def OUTGOING_BUFFER (atom []))
(def RAW_WRITER (atom nil))
(def PARSED_INCOMING_WRITER (atom nil))
(def OUTGOING_WRITER (atom nil))
(def FLUSH_THREAD (atom nil))

(def BUFFER_SIZE 100)
(def FLUSH_INTERVAL_MS 5000)

(defn- format-raw-entry
  [{:keys [timestamp data]}]
  (str timestamp " " data "\n"))

(defn- format-packet-entry
  [{:keys [timestamp packet]}]
  (str "{:timestamp " timestamp " :packet " (pr-str packet) "}\n"))

(def streams
  {:raw
   {:file "raw-bytes.txt"
    :fmt format-raw-entry
    :data-key :data
    :buffer RAW_BYTES_BUFFER
    :writer RAW_WRITER}
   :parsed
   {:file "parsed-incoming.edn"
    :fmt format-packet-entry
    :data-key :packet
    :buffer PARSED_INCOMING_BUFFER
    :writer PARSED_INCOMING_WRITER}
   :outgoing
   {:file "outgoing.edn"
    :fmt format-packet-entry
    :data-key :packet
    :buffer OUTGOING_BUFFER
    :writer OUTGOING_WRITER}})

(declare start-flush-thread! log-parsed-incoming)
(declare flush-all-buffers! flush-stream!)

(defn init-packet-logging!
  "Create logs directory, open buffered writers for all streams, and start the auto-flush thread."
  ([]
   (init-packet-logging! "logs"))
  ([dir]
   (when (and *packet-logging* (nil? @RAW_WRITER))
     (let [dir-file (io/file dir)]
       (.mkdirs dir-file)
       (doseq [[k {:keys [file writer]}] streams]
         (reset! writer (io/writer (io/file dir file) :append true :buffer-size 65536)))
       (start-flush-thread!)
       (println "clj-insim: packet logging initialized in" dir)))))

(defn stop-packet-logging!
  "Stop the auto-flush thread, flush all buffers to disk, and close all writers."
  []
  (when (and *packet-logging* (boolean @RAW_WRITER))
    (when @FLUSH_THREAD
      (future-cancel @FLUSH_THREAD)
      (reset! FLUSH_THREAD nil))
    (flush-all-buffers!)
    (doseq [[k {:keys [writer]}] streams]
      (.close @writer)
      (reset! writer nil))
    (println "clj-insim: packet logging stopped")))

(defn- hex-dump
  "Convert byte array to readable hex format: \"[7E 00 FF] (3 bytes)\"."
  [bytes]
  (let [len (count bytes)]
    (str "["
         (str/join " " (map (fn [b] (format "%02X" (bit-and b 0xFF))) bytes))
         "] (" len " bytes)")))

(defn- log!
  "Queue entry to a stream's buffer, building {:timestamp ... data-key} dict.
   Auto-flushes when buffer reaches BUFFER_SIZE."
  [stream-key data]
  (when *packet-logging*
    (let [{:keys [buffer data-key]} (streams stream-key)]
      (swap! buffer conj
             (assoc {:timestamp (System/currentTimeMillis)} data-key data))
      (when (>= (count @buffer) BUFFER_SIZE)
        (flush-stream! stream-key)))))

(defn log-raw-bytes
  "Queue raw TCP bytes (hex-formatted) to the :raw stream buffer for disk writing."
  [bytes]
  (log! :raw (hex-dump bytes)))

(defn log-parsed-incoming
  "Queue decoded packet to the :parsed stream buffer for disk writing."
  [packet]
  (log! :parsed packet))

(defn log-outgoing
  "Queue outgoing packet to the :outgoing stream buffer for disk writing."
  [packet]
  (log! :outgoing packet))

(defn- flush-stream!
  "Atomically drain a stream buffer and write formatted entries to disk."
  [stream-key]
  (let [{:keys [buffer writer fmt]} (streams stream-key)
        entries                     (first (swap-vals! buffer (constantly [])))]
    (when (seq entries)
      (doseq [entry entries]
        (.write @writer (fmt entry)))
      (.flush @writer))))

(defn flush-all-buffers!
  "Synchronously flush all stream buffers to disk via the registry."
  []
  (doseq [k (keys streams)]
    (flush-stream! k)))

(defn- start-flush-thread!
  "Start a background thread that flushes all buffers every 5 seconds."
  []
  (reset! FLUSH_THREAD
    (future
      (loop []
        (Thread/sleep FLUSH_INTERVAL_MS)
        (when *packet-logging*
          (try
            (flush-all-buffers!)
            (catch Exception e
              (println "clj-insim: error flushing logs:" (.getMessage e))))
          (recur))))))

(defn print-verbose
  "Print packet to stdout if @VERBOSE is true, and always queue for logging."
  [packet]
  (when @VERBOSE
    (newline)
    (println (str "IS_" (-> (:header/type packet) name str/upper-case) " packet!"))
    (println (str packet)))
  (log-parsed-incoming packet))

(defn log-throwable
  "Record exception to ERROR_LOG and print message to stdout."
  [t]
  (when @ERRORS
    (swap! ERROR_LOG conj (Throwable->map t))
    (println "clj-insim error:" (.getMessage t))))

(defn wrap-try-catch
  "Execute f with args, catching and logging any exceptions."
  [f & args]
  (try (apply f args) (catch Throwable t (log-throwable t))))
