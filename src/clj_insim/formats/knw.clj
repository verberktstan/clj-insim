(ns clj-insim.formats.knw
  "Parser for LFS `.knw` (AI knowledge) files.

   This format is not publicly documented; the layout below is
   reverse-engineered from a single sample (`BL1_XFG.knw`) and cross-checked
   against `BL1.pth`:

   - The 36-byte header's trailing value is a segment count that matches the
     number of 24-byte records actually present in the file.
   - Each segment record's 3rd/4th fields are a `[start end)` range that
     chains across records (record i's end == record i+1's start), forming a
     closed loop over path-node indices - i.e. LFS divides the lap into
     driving segments (corners + following straights).
   - The two trailing floats per segment are an open hypothesis, NOT
     confirmed: `curvature-hyp` was tested against geometric curvature
     computed from `pth.clj` and only weakly correlated (Spearman ~0.1-0.2 on
     BL1/XFG) - treat its meaning as unresolved, not as curvature.

   Reads via `java.nio.ByteBuffer` rather than `marshal.core` - see the
   docstring in `clj-insim.formats.pth` for why (marshal's `m/float` throws
   on negative floats, which this data is full of).

   See PTH_KNW_FORMAT_RECAP.md for the full writeup and the validation that
   produced (and partly refuted) these hypotheses."
  (:require [clojure.java.io :as io])
  (:import [java.nio ByteBuffer ByteOrder]))

(def ^:private HEADER_SIZE 36)
(def ^:private SEGMENT_SIZE 24)

(defn- ^ByteBuffer file->buffer [path]
  (let [bytes (with-open [in (io/input-stream (io/file path))]
                (.readAllBytes in))]
    (doto (ByteBuffer/wrap bytes)
      (.order ByteOrder/LITTLE_ENDIAN))))

(defn- read-header
  "Reads header fields via ordered `let` bindings, not an inline map literal -
   these calls mutate `buf`'s read position as a side effect, and map literal
   values are not guaranteed to evaluate left-to-right."
  [^ByteBuffer buf]
  (let [magic (byte-array 6)
        _ (.get buf magic)
        version (bit-and 0xFF (.get buf))
        revision (bit-and 0xFF (.get buf))
        unknown-1 (.getInt buf)
        unknown-2 (.getInt buf)
        unknown-3 (.getFloat buf)
        top-speed-hyp (.getFloat buf) ;; hypothesis: learned top speed, m/s
        unknown-4 (.getFloat buf)
        unknown-5 (.getFloat buf)
        ;; matches the corresponding .pth's node count in our one sample
        path-node-count (bit-and 0xFFFF (.getShort buf))
        segment-count (bit-and 0xFFFF (.getShort buf))]
    {:knw/magic (String. magic "US-ASCII")
     :knw/version version
     :knw/revision revision
     :knw/unknown-1 unknown-1
     :knw/unknown-2 unknown-2
     :knw/unknown-3 unknown-3
     :knw/top-speed-hyp top-speed-hyp
     :knw/unknown-4 unknown-4
     :knw/unknown-5 unknown-5
     :knw/path-node-count path-node-count
     :knw/segment-count segment-count}))

(defn- read-segment
  "See `read-header` docstring re: ordered `let` over a map literal."
  [^ByteBuffer buf]
  (let [reserved (.getInt buf) ;; always 0 in our one sample
        flags (.getInt buf) ;; bit-packed, semantics unresolved
        start-node (.getInt buf)
        end-node (.getInt buf)
        curvature-hyp (.getFloat buf) ;; NOT confirmed - see ns docstring
        unknown (.getFloat buf)]
    {:segment/reserved reserved
     :segment/flags flags
     :segment/start-node start-node
     :segment/end-node end-node
     :segment/curvature-hyp curvature-hyp
     :segment/unknown unknown}))

(defn parse
  "Parse a `.knw` file at `path` into `{:header ... :segments [...]}`."
  [path]
  (let [buf (file->buffer path)
        header (read-header buf)]
    {:header header
     :segments (vec (repeatedly (:knw/segment-count header) #(read-segment buf)))}))
