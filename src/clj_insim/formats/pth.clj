(ns clj-insim.formats.pth
  "Parser for LFS `.pth` (path) files.

   The header/node layout below is reverse-engineered, not from an official
   spec: verified geometrically by checking that each node's stored direction
   vector aligns with the position delta to the next node (average cosine
   similarity 0.996 across BL1.pth's 549 nodes). Byte offsets that don't
   affect that check (the 20-byte node tail: presumed flags + track-width
   limits) are read raw and left unparsed.

   This ns reads directly via `java.nio.ByteBuffer` instead of the project's
   usual `marshal.core` codecs: marshal's `m/float` doesn't sign-mask the top
   byte before handing the accumulated bits to `Float/intBitsToFloat`, so any
   negative float (sign bit set) throws `Value out of range for int`. Track
   direction/curvature data is full of negative floats, so that bug isn't
   avoidable while using marshal here.

   See PTH_KNW_FORMAT_RECAP.md for the full writeup."
  (:require [clojure.java.io :as io])
  (:import [java.nio ByteBuffer ByteOrder]))

(def ^:private HEADER_SIZE 12)
(def ^:private NODE_SIZE 44)

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
        flags (.getInt buf)]
    {:pth/magic (String. magic "US-ASCII")
     :pth/version version
     :pth/revision revision
     :pth/flags flags}))

(defn- read-node [^ByteBuffer buf]
  (let [pos-x (.getInt buf)
        pos-y (.getInt buf)
        pos-z (.getInt buf)
        dir-x (.getFloat buf)
        dir-y (.getFloat buf)
        dir-z (.getFloat buf)
        tail (byte-array 20)]
    (.get buf tail)
    {:node/pos-x pos-x
     :node/pos-y pos-y
     :node/pos-z pos-z
     :node/dir-x dir-x
     :node/dir-y dir-y
     :node/dir-z dir-z
     :node/tail (vec tail)}))

(defn parse
  "Parse a `.pth` file at `path` into `{:header ... :nodes [...]}`.
   Node count is derived from file size, since it isn't (yet) a field we've
   located in the header."
  [path]
  (let [buf (file->buffer path)
        header (read-header buf)
        node-count (quot (- (.limit buf) HEADER_SIZE) NODE_SIZE)]
    {:header header
     :nodes (vec (repeatedly node-count #(read-node buf)))}))

(defn position-metres
  "Node position as `[x y z]` in metres."
  [{:node/keys [pos-x pos-y pos-z]}]
  [(/ pos-x 65536.0) (/ pos-y 65536.0) (/ pos-z 65536.0)])

(defn direction
  "Node direction as a `[x y z]` unit vector."
  [{:node/keys [dir-x dir-y dir-z]}]
  [dir-x dir-y dir-z])
