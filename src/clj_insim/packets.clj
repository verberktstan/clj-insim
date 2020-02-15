(ns clj-insim.packets
  (:require [clj-insim.models.packet :as packet]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Helpers

(defn- c-stringify [s]
  (let [new-s (str s (char 0))]
    (if (zero? (-> new-s count (mod 4)))
      new-s
      (recur new-s))))

(defn- ->c-string [s max-length]
  (if (>= (count s) max-length)
    (str (subs s 0 (dec max-length)) (char 0))
    s))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Packets

(defn insim-init
  ([]
   (insim-init {}))
  ([{:keys [admin iname]}]
   {::packet/header
    {:size 44 :type :isi :request-info 1 :data 0}
    ::packet/body
    {:udp-port 0 :flags 0 :insim-version 8 :prefix (int \!) :interval 0
     :admin (apply str (take 16 (or admin "pwd")))
     :iname (apply str (concat (take 15 (or iname "clj-insim")) [(char 0)]))}}))

(defn tiny
  ([]
   (tiny {}))
  ([{:keys [request-info data]}]
   {::packet/header
    {:size 4 :type :tiny :request-info (or request-info 1) :data (or data :none)}}))

(def request-ncn #(tiny {:data :ncn}))
(def request-npl #(tiny {:data :npl}))
(def close #(tiny {:request-info 0 :data :close}))

(defn mst [message]
  {::packet/header
   {:size 68 :type :mst :request-info 0 :data 0}
   ::packet/body
   {:message (->c-string message 64)}})
