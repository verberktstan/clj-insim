(ns clj-insim.write
  (:require [clj-insim.codecs :as codecs]
            [clj-insim.models.packet :as packet]
            [clj-insim.parse :as parse]
            [marshal.core :as m]))

(defn- unknown-codec [{:header/keys [size]}]
  (when (> size 4)
    (m/struct :body/unkown (m/ascii-string (- size 4)))))

(defn- prepare
  "Prepare the packet by parsing the packet into a raw packet and determine the
   body codec."
  [{:header/keys [size type] :as packet}]
  {:pre [(or (nil? packet) (packet/parsed? packet))]}
  (when packet
    {:instruction (parse/instruction packet)
     :body-codec ((get codecs/body type unknown-codec) packet)}))

(defn- write-instruction!
  "Write raw instruction packet to output stream."
  [output-stream body-codec instruction]
  {:pre [(packet/raw? instruction)]}
  (m/write output-stream codecs/header instruction)
  (when body-codec
    (m/write output-stream body-codec instruction))
  (.flush output-stream))

(defn instruction
  "Prepares the packet and write it to the output stream."
  [output-stream packet]
  (let [{:keys [body-codec instruction]} (prepare packet)]
    (when instruction
      (write-instruction! output-stream body-codec instruction))))
