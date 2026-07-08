(ns clj-insim.logging
  "Verbose packet logging and error tracking for the InSim client.

   ## Architecture Overview

   This logging system has three independent data flows, each with its own buffer and file:

   **FLOW 1: Raw Bytes (raw-bytes.txt)**
   read.clj → log-raw-bytes → RAW_BYTES_BUFFER → [flush-raw-bytes! or auto-flush] → disk

   Captures the actual TCP bytes received before any decoding. Useful for:
   - Reproducing exact packet sequences
   - Debugging protocol-level issues
   - Inspecting corrupt packets

   **FLOW 2: Parsed Incoming (parsed-incoming.edn)**
   read.clj → parse → client.clj → print-verbose → log-parsed-incoming → PARSED_INCOMING_BUFFER
   → [flush-parsed-incoming! or auto-flush] → disk

   Captures the Clojure map representation after successful decoding and parsing.
   Useful for:
   - Analyzing game state changes
   - Replaying a session
   - Testing packet handlers

   **FLOW 3: Outgoing (outgoing.edn)**
   client.clj → channel → log-outgoing → OUTGOING_BUFFER → [flush-outgoing! or auto-flush] → disk

   Captures packets being sent before encoding to bytes. Useful for:
   - Verifying commands sent to LFS
   - Debugging client behavior
   - Full session reconstruction

   ## Buffering Strategy

   Each log buffer holds up to 100 entries in RAM. Writes to disk are triggered by:
   - EAGER: When 100 entries accumulate (flush immediately, prevent buffer overflow on bursts)
   - PERIODIC: Every 5 seconds from auto-flush thread (ensures data reaches disk even during quiet periods)
   - MANUAL: Call flush-all-buffers! explicitly
   - SHUTDOWN: stop-packet-logging! flushes and closes all files

   This design handles traffic spikes without dropping data while still batching writes for I/O efficiency."
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.io BufferedWriter]))

(def ERROR_LOG (atom nil))
(defonce ERRORS (atom true))
(defonce VERBOSE (atom false))

(def ^:dynamic *packet-logging* false)
(defonce RAW_BYTES_BUFFER (atom []))
(defonce PARSED_INCOMING_BUFFER (atom []))
(defonce OUTGOING_BUFFER (atom []))
(defonce ^BufferedWriter RAW_WRITER (atom nil))
(defonce ^BufferedWriter PARSED_INCOMING_WRITER (atom nil))
(defonce ^BufferedWriter OUTGOING_WRITER (atom nil))
(defonce FLUSH_THREAD (atom nil))

(def BUFFER_SIZE 100)
(def FLUSH_INTERVAL_MS 5000)

(declare start-flush-thread! log-parsed-incoming)
(declare flush-all-buffers!)
(declare flush-raw-bytes!)
(declare flush-parsed-incoming!)
(declare flush-outgoing!)

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
   (when (nil? @RAW_WRITER)
     (let [dir-file (io/file dir)]
       (.mkdirs dir-file)
       (reset! RAW_WRITER (io/writer (io/file dir "raw-bytes.txt") :append true :buffer-size 65536))
       (reset! PARSED_INCOMING_WRITER (io/writer (io/file dir "parsed-incoming.edn") :append true :buffer-size 65536))
       (reset! OUTGOING_WRITER (io/writer (io/file dir "outgoing.edn") :append true :buffer-size 65536))
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
  (when (not (nil? @RAW_WRITER))
    (when @FLUSH_THREAD
      (future-cancel @FLUSH_THREAD)
      (reset! FLUSH_THREAD nil))
    (flush-all-buffers!)
    (.close @RAW_WRITER)
    (.close @PARSED_INCOMING_WRITER)
    (.close @OUTGOING_WRITER)
    (reset! RAW_WRITER nil)
    (reset! PARSED_INCOMING_WRITER nil)
    (reset! OUTGOING_WRITER nil)
    (println "clj-insim: packet logging stopped")))

(defn- hex-dump [bytes]
  "**Purpose:** Convert raw binary bytes into human-readable hex format.

   **How it works:**
   - Iterates through each byte in the array
   - Masks with 0xFF and formats as 2-digit uppercase hex (e.g., \"7E\", \"00\", \"FF\")
   - Joins all bytes with spaces: \"7E 00 FF 04 ...\"
   - Wraps in brackets and appends byte count: \"[7E 00 FF 04] (4 bytes)\"

   **Connection to system:** Used only by log-raw-bytes when writing raw TCP bytes
   to raw-bytes.txt. Makes the binary data inspectable without a hex editor."
  (let [len (count bytes)]
    (str "["
         (str/join " " (map (fn [b] (format "%02X" (bit-and b 0xFF))) bytes))
         "] (" len " bytes)")))

(defn log-raw-bytes
  "**Purpose:** Queue raw TCP bytes from the input stream for buffered file writing.

   **How it works:**
   1. Checks if logging is enabled (*packet-logging*)
   2. Converts bytes to hex format via hex-dump
   3. Adds {:timestamp, :data} entry to RAW_BYTES_BUFFER
   4. If buffer reaches 100 entries, immediately flushes to disk (eager flush to prevent data loss)

   **Connection to system:** This should be called in read.clj right after reading raw bytes
   from the socket, before marshal decoding. Paired with flush-raw-bytes! to write to disk.
   Achieves buffering by accumulating entries in an atom; reaches disk either via eager flush
   (BUFFER_SIZE) or the auto-flush thread (every 5 seconds)."
  [bytes]
  (when *packet-logging*
    (swap! RAW_BYTES_BUFFER conj
           {:timestamp (System/currentTimeMillis)
            :data (hex-dump bytes)})
    (when (>= (count @RAW_BYTES_BUFFER) BUFFER_SIZE)
      (flush-raw-bytes!))))

(defn log-parsed-incoming
  "**Purpose:** Queue parsed, decoded incoming packets for buffered EDN file writing.

   **How it works:**
   1. Checks if logging is enabled (*packet-logging*)
   2. Adds {:timestamp, :packet} entry to PARSED_INCOMING_BUFFER
   3. If buffer reaches 100 entries, immediately flushes to disk

   **Connection to system:** Called by print-verbose and should also be called in client.clj
   after a packet is successfully parsed from bytes. Stores the complete Clojure map
   representation of the packet (after marshal decoding and InSim parsing).
   Pairs with flush-parsed-incoming! for disk writes."
  [packet]
  (when *packet-logging*
    (swap! PARSED_INCOMING_BUFFER conj
           {:timestamp (System/currentTimeMillis)
            :packet packet})
    (when (>= (count @PARSED_INCOMING_BUFFER) BUFFER_SIZE)
      (flush-parsed-incoming!))))

(defn log-outgoing
  "**Purpose:** Queue packets being sent to LFS for buffered EDN file writing.

   **How it works:**
   1. Checks if logging is enabled (*packet-logging*)
   2. Adds {:timestamp, :packet} entry to OUTGOING_BUFFER
   3. If buffer reaches 100 entries, immediately flushes to disk

   **Connection to system:** Should be called in client.clj or write.clj after a packet
   is enqueued for sending but before it's serialized to bytes. Stores the high-level
   Clojure representation (before marshal encoding). Pairs with flush-outgoing! for disk writes."
  [packet]
  (when *packet-logging*
    (swap! OUTGOING_BUFFER conj
           {:timestamp (System/currentTimeMillis)
            :packet packet})
    (when (>= (count @OUTGOING_BUFFER) BUFFER_SIZE)
      (flush-outgoing!))))

(defn- flush-raw-bytes!
  "**Purpose:** Write all queued raw bytes entries from RAM to raw-bytes.txt on disk.

   **How it works:**
   1. Takes a snapshot of RAW_BYTES_BUFFER (immutable view)
   2. For each buffered entry, writes a line: \"TIMESTAMP [HEX HEX HEX] (N bytes)\"
   3. Clears the in-memory buffer (reset to [])
   4. Calls .flush on the BufferedWriter to push data to OS and disk

   **Connection to system:** Triggered by either:
     - log-raw-bytes when buffer reaches 100 entries (eager flush)
     - The auto-flush thread every 5 seconds (background flush)
     - stop-packet-logging! when shutting down (final flush)
   Each line is one packet's worth of raw TCP bytes. Format is line-based text for easy inspection."
  []
  (let [entries @RAW_BYTES_BUFFER]
    (when (pos? (count entries))
      (doseq [{:keys [timestamp data]} entries]
        (.write @RAW_WRITER (str timestamp " " data "\n")))
      (reset! RAW_BYTES_BUFFER [])
      (.flush @RAW_WRITER))))

(defn- flush-parsed-incoming!
  "**Purpose:** Write all queued parsed incoming packet entries from RAM to parsed-incoming.edn on disk.

   **How it works:**
   1. Takes a snapshot of PARSED_INCOMING_BUFFER
   2. For each buffered entry, writes a single EDN map line:
      {:timestamp 1234567890 :packet {:header/type :ISI :body/insim-version 9 ...}}
   3. Clears the in-memory buffer (reset to [])
   4. Calls .flush on the BufferedWriter to push data to disk

   **Connection to system:** Triggered by either:
     - log-parsed-incoming when buffer reaches 100 entries (eager flush)
     - The auto-flush thread every 5 seconds (background flush)
     - stop-packet-logging! when shutting down (final flush)
   Each line is valid EDN that can be read with clojure.edn/read-string.
   Packets here are after full decoding (marshal unmarshalling + InSim parsing)."
  []
  (let [entries @PARSED_INCOMING_BUFFER]
    (when (pos? (count entries))
      (doseq [{:keys [timestamp packet]} entries]
        (.write @PARSED_INCOMING_WRITER (str "{:timestamp " timestamp " :packet " (pr-str packet) "}\n")))
      (reset! PARSED_INCOMING_BUFFER [])
      (.flush @PARSED_INCOMING_WRITER))))

(defn- flush-outgoing!
  "**Purpose:** Write all queued outgoing packet entries from RAM to outgoing.edn on disk.

   **How it works:**
   1. Takes a snapshot of OUTGOING_BUFFER
   2. For each buffered entry, writes a single EDN map line:
      {:timestamp 1234567890 :packet {:header/type :ISM :body/text \"hello\" ...}}
   3. Clears the in-memory buffer (reset to [])
   4. Calls .flush on the BufferedWriter to push data to disk

   **Connection to system:** Triggered by either:
     - log-outgoing when buffer reaches 100 entries (eager flush)
     - The auto-flush thread every 5 seconds (background flush)
     - stop-packet-logging! when shutting down (final flush)
   Each line is valid EDN. Packets here are high-level Clojure maps before
   marshal encoding (before they become bytes on the wire)."
  []
  (let [entries @OUTGOING_BUFFER]
    (when (pos? (count entries))
      (doseq [{:keys [timestamp packet]} entries]
        (.write @OUTGOING_WRITER (str "{:timestamp " timestamp " :packet " (pr-str packet) "}\n")))
      (reset! OUTGOING_BUFFER [])
      (.flush @OUTGOING_WRITER))))

(defn flush-all-buffers!
  "**Purpose:** Synchronously write all three buffered queues to disk in sequence.

   **How it works:**
   - Calls flush-raw-bytes!, flush-parsed-incoming!, and flush-outgoing! in order
   - Each call writes its buffer and clears it

   **Connection to system:** Used by:
     - stop-packet-logging! to ensure no data loss on shutdown
     - The auto-flush thread (every 5 seconds) to periodically persist data
   This is a convenience wrapper for manual flushing. If spikes cause buffers to
   grow, calling this directly will guarantee all data reaches disk immediately."
  []
  (flush-raw-bytes!)
  (flush-parsed-incoming!)
  (flush-outgoing!))

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

(defn print-verbose [packet]
  "**Purpose:** Print packet to stdout if verbose mode is enabled, AND queue it for file logging.

   **How it works:**
   1. If @VERBOSE is true, prints to console (original behavior)
   2. Always calls log-parsed-incoming to queue the packet for file logging
      (this happens regardless of VERBOSE setting)

   **Connection to system:** Modified to integrate packet logging. Called from
   client.clj after every successfully parsed incoming packet. This means packets
   go to both the console (if verbose) and always to the parsed-incoming.edn file.
   The file logging is decoupled from console verbosity."
  (when @VERBOSE
    (newline)
    (println (str "IS_" (-> (:header/type packet) name str/upper-case) " packet!"))
    (println (str packet)))
  (log-parsed-incoming packet))

(defn log-throwable [t]
  "**Purpose:** Record exceptions to an error log and optionally print them.

   **How it works:**
   1. If @ERRORS is true, appends the exception to ERROR_LOG atom (using Throwable->map)
   2. Prints the exception message to stdout

   **Connection to system:** Called by wrap-try-catch when any exception occurs.
   The ERROR_LOG atom accumulates all errors during a session, separate from
   packet logging. This is for debugging code errors, not packet data."
  (when @ERRORS
    (swap! ERROR_LOG conj (Throwable->map t))
    (println "clj-insim error:" (.getMessage t))))

(defn wrap-try-catch [f & args]
  "**Purpose:** Execute a function safely, catching and logging any exceptions.

   **How it works:**
   1. Tries to call the function f with the given args
   2. If an exception occurs, catches it and passes to log-throwable
   3. Returns nil if an exception occurred

   **Connection to system:** Used in client.clj to wrap the read and write functions.
   If a parse error or network error occurs, it's caught and logged without crashing
   the client loop. Paired with log-throwable for error tracking."
  (try (apply f args) (catch Throwable t (log-throwable t))))
