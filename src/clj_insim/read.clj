(ns clj-insim.read
  (:require [clj-insim.codecs :as codecs]
            [clj-insim.models.packet :as packet]
            [clj-insim.parse :as parse]
            [marshal.core :as m]))

(defn- read-packet [input-stream]
  (let [header (-> input-stream (m/read codecs/header) parse/header)]
    (packet/make
     header
     (when (> (:size header) 4)
       (-> input-stream (m/read (codecs/body header)) parse/body)))))

(defn packets
  "Returns packets (based on all available bytes) read from input-stream"
  [input-stream]
  (loop [available-bytes (.available input-stream)
         result []]
    (if (not (pos? available-bytes))
      (seq result)
      (let [packet (read-packet input-stream)]
        (recur (.available input-stream) (conj result packet))))))
