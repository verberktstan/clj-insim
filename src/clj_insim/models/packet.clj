(ns clj-insim.models.packet
  (:refer-clojure :exclude [read])
  (:require [clojure.spec.alpha :as s]
            [clj-insim.codecs :as codecs]
            [clj-insim.enums :as enums]
            [clj-insim.parse :as parse]
            [marshal.core :as m]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Specs

(s/def ::header (s/keys :req-un [::size ::type ::request-info ::data]))
(s/def ::body map?)

(s/def ::model
  (s/keys :req [::header]
          :opt [::body]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Reading packets

(defn- read-header [input-stream]
  (let [{:keys [type] :as header}
        (-> input-stream
            (m/read codecs/header)
            (update :type parse/isp))]
    (parse/header header)))

(defn- read-body [header input-stream]
  (m/read input-stream (codecs/body header)))

(defn- make [header body]
  ;{:post [(s/valid? ::model %)]}
  (merge {::header header} (when body {::body body})))

(defn read [input-stream]
  (let [{:keys [size] :as header} (read-header input-stream)]
    (make header (when (> size 4) (read-body header input-stream)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Writing packets

(defn- write-header [output-stream {::keys [header]}]
  (let [{:keys [type]} header]
    (m/write
     output-stream
     codecs/header
     (-> header
         (update :type parse/unparse-isp)
         (update :data (case type
                         :tiny parse/unparse-tiny
                         :small parse/unparse-small
                         :ttc parse/unparse-ttc
                         identity))))))

(defn- write-body [output-stream {::keys [header body]}]
  (m/write output-stream (codecs/body header) body))

(defn write [output-stream packets]
  (doseq [packet packets]
    (doto output-stream
      (write-header packet)
      (write-body packet)))
  (.flush output-stream))
