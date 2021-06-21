(ns clj-insim.read
  (:require [clj-insim.codecs :as codecs]
            [clj-insim.parse :as parse]
            [clj-insim.utils :as u]
            [marshal.core :as m]))

(defn- read-header
  "Reads 4 bytes from input-stream and returns these, marhalled into a clojure
   map with the header codec. Returns false when no bytes are available to read."
  [input-stream]
  (and (pos? (.available input-stream))
       (m/read input-stream codecs/header)))

(def ^:private header (comp parse/header read-header))

(defn- get-body-codec
  "Returns the marshal codec for a given header (type).
   When the codec for this type can't be found, it returns a default unknown codec."
  [{:header/keys [type] :as header}]
  (let [codec (get
               codecs/body
               type
               (fn [{:header/keys [size]}]
                 (m/struct :body/unknown (m/ascii-string (- size 4)))))]
    (codec header)))

(defn- read-body [input-stream {:header/keys [size] :as header}]
  (when (> size 4)
    (m/read input-stream (get-body-codec header))))

(defn- body [input-stream header]
  (-> (read-body input-stream header)
      (merge header)
      (parse/body)))

(defn packet
  "Read (info) packet header and body from input-stream, parse and return it.
   Returns `nil` when no data is present on the input-stream."
  [input-stream]
  (when-let [hdr (header input-stream)]
    (body input-stream hdr)))
