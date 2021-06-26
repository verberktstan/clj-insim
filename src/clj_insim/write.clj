(ns clj-insim.write
  (:require [clj-insim.codecs :as codecs]
            [clj-insim.models.packet :as packet]
            [clj-insim.parse :as parse]
            [marshal.core :as m]))

(defn- unknown-codec-fn [size]
  (m/struct :body/unkown (m/ascii-string (- size 4))))

(defn packet!
  "Parse the packet as instruction (packet to send to LFS) and write it to the
   output-stream."
  [output-stream {:header/keys [size type] :as packet}]
  {:pre [(packet/parsed? packet)]}
  (let [instruction (parse/instruction packet)
        body-codec-fn (get codecs/body type unknown-codec-fn)]
    (m/write output-stream codecs/header instruction)
    (when (> size 4)
      (m/write output-stream (body-codec-fn packet) instruction))
    (.flush output-stream)))
