(ns clj-insim.write
  (:require [clj-insim.codecs :as codecs]
            [clj-insim.models.packet :as packet]
            [clj-insim.parse :refer [unparse unparse-body]]
            [marshal.core :as m]))

(defn- write-header [output-stream {::packet/keys [header]}]
  (let [{:keys [type]} header]
    (m/write
     output-stream
     codecs/header
     (unparse header))))

(defn- write-body [output-stream {::packet/keys [header body]}]
  (m/write
   output-stream
   (codecs/body header)
   (unparse-body body)))

(defn packets [output-stream packets]
  (doseq [packet packets]
    (doto output-stream
      (write-header packet)
      (write-body packet)))
  (.flush output-stream))
