(ns clj-insim.write
  (:require [clj-insim.codecs :as codecs]
            [clj-insim.parse :as parse]
            [marshal.core :as m]))

(defn packet!
  "Parse the packet as instruction (packet to send to LFS) and write it to the
   output-stream."
  [output-stream {:header/keys [size type] :as packet}]
  (let [instruction (parse/instruction packet)
        body-codec (get codecs/body type #(m/struct :body/unkown (m/ascii-string (- size 4))))]
    (m/write output-stream codecs/header instruction)
    (when (> size 4)
      (m/write output-stream (body-codec packet) instruction))
    (.flush output-stream)))
