(ns clj-insim.core
  (:require [clj-sockets.core :as sockets]
            [clojure.java.io :as io]
            [clj-insim.enums :as enums]
            [clj-insim.packets :as packets]
            [clj-insim.parsers :refer [parse-packet]]
            [clj-insim.protocols :refer [is-protocols]]
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
          (let [m (parse-packet packet (is-protocols :ver))]
            (println (str m))
            (packets/is-mst "Hello from clj-insim!")))
   :tiny (print-keepalive-dispatch (is-protocols :tiny))
   :mso (print-keepalive-dispatch (is-protocols :mso))
   :sta (print-keepalive-dispatch (is-protocols :sta))
   :flg (print-keepalive-dispatch (is-protocols :flg))
   :csc (print-keepalive-dispatch (is-protocols :csc))
;   :npl (print-keepalive-dispatch (is-protocols :npl))
   :npl (fn [packet]
          (let [{:keys [handicap-mass] :as m} (parse-packet packet (is-protocols :npl))]
            (if (< handicap-mass 15)
              (packets/is-jrr (assoc
                                  (select-keys m [:player-id :uniq-connection-id])
                                :jrr-action (enums/jrr-action :reject)))
              (packets/is-tiny))))})

;; Simple hander prints the type of packet received and returns a IS_TYNI/none packet to maintain connection
(defn simple-handler [packet]
  (let [[type] packet
        type-key (enums/isp-key (int type))
        f (type-dispatch type-key)]
    (do
      (println (str "=== Received " (name type-key) " packet from LFS ==="))
      (if f ; Execute dispatch OR return keepalive packet
        (f packet)
        (packets/is-tiny)))))

(comment
  ;; Start a tcp client with simple-handler
                                        ;  (def simple-server (serve HOST PORT simple-handler))
  (def simple-server (serve simple-handler))
  ;; To stop the client
  (reset! simple-server false)

  (def npl-packet {:uniq-connection-id 0, :driver-model 30, :player-type 0, :spare-3 0, :car-name "FZR", :setup-flags 0, :number-player 1, :spare-2 0, :handicap-mass 0, :type :npl, :player-id 2, :skin-name "ARCHLINUX1", :passenger 0, :handicap-restriction 0, :reqi 0, :license-plate "", :flags (72 18), :player-name "Van Sterberkt", :tyres (2 2 1 1), :spare (0 0 0 0)})
)
