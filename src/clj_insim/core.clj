(ns clj-insim.core
  (:require [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parsers :refer [parse-packet]]
            [clj-insim.protocols :refer [is-protocols]]
            [clj-insim.socket :refer [serve]]
            [clj-insim.util :as util])
  (:import [java.nio ByteBuffer]))

(defn welcome []
  (packets/is-mst "Hello from clj-insim!"))

(defn close-connection []
  (packets/is-tiny {:data-key :close}))

(defn check-version [{:keys [version insim-version]}]
  (if (and (= version "0.6T")
           (>= insim-version 7))
    (welcome)
    (close-connection)))

(def dispatchers
  {:ver check-version})

(defn dispatch [{:keys [type] :as incoming}]
  (let [f (type dispatchers)]
    (do (prn incoming)
      (if f
        (f incoming)
        (packets/is-tiny)))))

(defn parse [type-key packet]
  (parse-packet packet (is-protocols type-key)))

;; Simple hander prints the type of packet received and returns a IS_TYNI/none packet to maintain connection
(defn handler [packet]
  (let [[type] packet
        type-key (enums/isp-key (int type))
        incoming (parse type-key packet)]
    (do
      (println "=== Received " (name type-key) " packet from LFS ===")
      (if incoming
        (dispatch incoming)
        (packets/is-tiny)))))

(comment
  ;; Start a tcp client with simple-handler
  (def simple-server (serve handler))
  ;; To stop the client
  (reset! simple-server false)

  (:test nil)
)
