(ns clj-insim.read
  (:require [clj-insim.codecs :as codecs]
            [clj-insim.models.packet :as packet]
            [clj-insim.parse :as parse]
            [marshal.core :as m]))

(defn- read-header
  "Reads 4 bytes from input-stream and returns these, marshalled into a clojure
   map with the header codec. Returns false when no bytes are available to read."
  [input-stream]
  {:post [(or (nil? %) (packet/raw? %))]}
  (when (pos? (.available input-stream))
    (m/read input-stream codecs/header)))

(defn- header
  "Read and parse header data from input-stream. Multiplies :header/size by
   size-mul on the fly."
  [size-mul input-stream]
  (-> input-stream
      read-header
      parse/header
      (update :header/size (comp int (partial * size-mul)))))

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
  "Returns a function that reads (info) packet header and body from input-stream,
   parse and return it. The returned fn returns `nil` when no data is present on
   the input-stream."
  [new-byte-size?]
  (let [header-fn (if new-byte-size? (partial header 4) (partial header 1))]
    (fn read-packet
      [input-stream]
      {:post [(or (nil? %) (packet/parsed? %))]}
      (when-let [hdr (header-fn input-stream)]
        (body input-stream hdr)))))
