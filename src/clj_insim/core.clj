(ns clj-insim.core
  (:require [clj-sockets.core :as sockets]
            [clojure.java.io :as io]
            [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parsers :refer [parse-packet]]
            [clj-insim.protocols :as protocols]
            [clj-insim.socket :refer [serve]]
            [clj-insim.util :as util])
  (:import [java.nio ByteBuffer]))

(defn print-keepalive-dispatch [protocol]
  (fn [packet]
    (let [m (parse-packet packet protocol)]
      (prn m)
      (packets/is-tiny))))

(def type-dispatch
  {:ver (fn [packet]
          (let [m (parse-packet packet protocols/is-ver-protocol)]
            (println (str m))
            (packets/is-mst-packet "Hello from clj-insim!")))
   :tiny (print-keepalive-dispatch protocols/is-tiny-protocol)
   :mso (print-keepalive-dispatch protocols/is-mso-protocol)
   :sta (print-keepalive-dispatch protocols/is-sta-protocol)
   :flg (print-keepalive-dispatch protocols/is-flg-protocol)
   :csc (print-keepalive-dispatch protocols/is-csc-protocol)
   :npl (print-keepalive-dispatch protocols/is-npl-protocol)})

;; Simple hander prints the type of packet received and returns a IS_TYNI/none packet to maintain connection
(defn simple-handler [packet]
  (let [[type] packet
        type-key (enums/isp-key (int type))
        f (type-dispatch type-key)]
    (do
      (println (str "===== Received " (name type-key) " packet from LFS ====="))
      (if f ; Execute dispatch OR return keepalive packet
        (f packet)
        (packets/is-tiny)))))

(comment
  ;; Start a tcp client with simple-handler
                                        ;  (def simple-server (serve HOST PORT simple-handler))
  (def simple-server (serve simple-handler))
  ;; To stop the client
  (reset! simple-server false)
)
 
