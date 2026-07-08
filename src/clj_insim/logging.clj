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
  "**Purpose:** Set up the entire packet logging infrastructure.

   **How it works:**
   1. Creates a logs directory (or specified dir) if it doesn't exist
   2. Opens three buffered file writers with 64KB internal buffers:
      - raw-bytes.txt: Raw incoming TCP bytes in hex format
      - parsed-incoming.edn: Clojure maps of received packets
      - outgoing.edn: Clojure maps of sent packets
   3. Enables the global PACKET_LOGGING flag
   4. Starts a background thread that auto-flushes every 5 seconds

   **Connection to system:** This is the entry point. Call this once at startup.
   All logging functions check *packet-logging* before writing, so nothing happens
   until this is called. The buffered writers hold data in RAM until flush operations
   write to disk (either triggered by reaching BUFFER_SIZE=100 entries or the auto-flush thread)."
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
  "**Purpose:** Cleanly shut down the logging system and ensure no data loss.

   **How it works:**
   1. Cancels the background flush thread (stops periodic flushing)
   2. Calls flush-all-buffers! to write any remaining queued data to disk
   3. Closes all three file writers, finalizing the files
   4. Resets all atom states back to nil/false
   5. Disables *packet-logging* to prevent new log calls from executing

   **Connection to system:** Call this when shutting down the client or when you're
   done testing. Without this, in-memory buffered data may be lost and file handles
   will remain open. Safe to call multiple times (checks *packet-logging* first)."
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
  "**Purpose:** Convert raw binary bytes into human-readable hex format.

   **How it works:**
   - Iterates through each byte in the array
   - Masks with 0xFF and formats as 2-digit uppercase hex (e.g., \"7E\", \"00\", \"FF\")
   - Joins all bytes with spaces: \"7E 00 FF 04 ...\"
   - Wraps in brackets and appends byte count: \"[7E 00 FF 04] (4 bytes)\"

   **Connection to system:** Used only by log-raw-bytes when writing raw TCP bytes
   to raw-bytes.txt. Makes the binary data inspectable without a hex editor."
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
  "**Purpose:** Queue raw TCP bytes from the input stream for buffered file writing.

   **How it works:**
   1. Checks if logging is enabled (*packet-logging*)
   2. Converts bytes to hex format via hex-dump
   3. Adds {:timestamp, :data} entry to RAW_BYTES_BUFFER
   4. If buffer reaches 100 entries, immediately flushes to disk (eager flush to prevent data loss)

   **Connection to system:** This should be called in read.clj right after reading raw bytes
   from the socket, before marshal decoding. Data reaches disk either via eager flush
   (BUFFER_SIZE) or the auto-flush thread (every 5 seconds)."
  [bytes]
  (log! :raw (hex-dump bytes)))

(defn log-parsed-incoming
  "**Purpose:** Queue parsed, decoded incoming packets for buffered EDN file writing.

   **How it works:**
   1. Checks if logging is enabled (*packet-logging*)
   2. Adds {:timestamp, :packet} entry to PARSED_INCOMING_BUFFER
   3. If buffer reaches 100 entries, immediately flushes to disk

   **Connection to system:** Called by print-verbose and should also be called in client.clj
   after a packet is successfully parsed from bytes. Stores the complete Clojure map
   representation of the packet (after marshal decoding and InSim parsing)."
  [packet]
  (log! :parsed packet))

(defn log-outgoing
  "**Purpose:** Queue packets being sent to LFS for buffered EDN file writing.

   **How it works:**
   1. Checks if logging is enabled (*packet-logging*)
   2. Adds {:timestamp, :packet} entry to OUTGOING_BUFFER
   3. If buffer reaches 100 entries, immediately flushes to disk

   **Connection to system:** Should be called in client.clj or write.clj after a packet
   is enqueued for sending but before it's serialized to bytes. Stores the high-level
   Clojure representation (before marshal encoding)."
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
  "**Purpose:** Start a background daemon thread that periodically flushes all buffers.

   **How it works:**
   1. Creates a future (background thread) that runs an infinite loop
   2. Every 5000ms (FLUSH_INTERVAL_MS), wakes up and calls flush-all-buffers!
   3. Wraps in try-catch to avoid crashing on I/O errors; logs errors instead
   4. Only loops when *packet-logging* is true (stops when logging is disabled)
   5. Stores the future in FLUSH_THREAD atom so it can be cancelled later

   **Connection to system:** Started by init-packet-logging! and runs until
   stop-packet-logging! cancels it. Handles the \"buffering\" part of the system:
   even if burst traffic doesn't fill the 100-entry buffers, this thread ensures
   data hits disk every 5 seconds. This trades latency for efficiency—data isn't
   written immediately on every packet, but stays fresh on disk."
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
  "**Purpose:** Print packet to stdout if verbose mode is enabled, AND queue it for file logging.

   **How it works:**
   1. If @VERBOSE is true, prints to console (original behavior)
   2. Always calls log-parsed-incoming to queue the packet for file logging
      (this happens regardless of VERBOSE setting)

   **Connection to system:** Modified to integrate packet logging. Called from
   client.clj after every successfully parsed incoming packet. This means packets
   go to both the console (if verbose) and always to the parsed-incoming.edn file.
   The file logging is decoupled from console verbosity."
  [packet]
  (when @VERBOSE
    (newline)
    (println (str "IS_" (-> (:header/type packet) name str/upper-case) " packet!"))
    (println (str packet)))
  (log-parsed-incoming packet))

(defn log-throwable
  "**Purpose:** Record exceptions to an error log and optionally print them.

   **How it works:**
   1. If @ERRORS is true, appends the exception to ERROR_LOG atom (using Throwable->map)
   2. Prints the exception message to stdout

   **Connection to system:** Called by wrap-try-catch when any exception occurs.
   The ERROR_LOG atom accumulates all errors during a session, separate from
   packet logging. This is for debugging code errors, not packet data."
  [t]
  (when @ERRORS
    (swap! ERROR_LOG conj (Throwable->map t))
    (println "clj-insim error:" (.getMessage t))))

(defn wrap-try-catch
  "**Purpose:** Execute a function safely, catching and logging any exceptions.

   **How it works:**
   1. Tries to call the function f with the given args
   2. If an exception occurs, catches it and passes to log-throwable
   3. Returns nil if an exception occurred

   **Connection to system:** Used in client.clj to wrap the read and write functions.
   If a parse error or network error occurs, it's caught and logged without crashing
   the client loop. Paired with log-throwable for error tracking."
  [f & args]
  (try (apply f args) (catch Throwable t (log-throwable t))))
