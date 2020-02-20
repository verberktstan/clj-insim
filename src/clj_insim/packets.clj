(ns clj-insim.packets
  (:require [clj-insim.models.packet :as packet]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Helpers

(defn- ->c-string [s max-length]
  (-> (apply str s (repeat max-length (char 0)))
      (subs 0 (dec max-length))
      (str (char 0))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Packets

(defn insim-init
  ([]
   (insim-init {}))
  ([params]
   {::packet/header
    {:size 44 :type :isi :request-info 1 :data 0}
    ::packet/body
    (-> (merge
         {:isi/udp-port 0 :isi/flags #{} :isi/insim-version 8 :isi/prefix (int \!) :isi/interval 0
          :isi/admin "pwd"
          :isi/iname "clj-insim"}
         params)
        (update :isi/admin ->c-string 16)
        (update :isi/iname ->c-string 16))}))

(defn tiny
  ([]
   (tiny {}))
  ([params]
   {::packet/header
    (merge
     {:size 4 :type :tiny :request-info 1 :data :none}
     params)}))

(comment
  (tiny {:request-info 0 :data :close})
  (tiny {:data :ncn})
)

(defn mst [message]
  {::packet/header
   {:size 68 :type :mst :request-info 0 :data 0}
   ::packet/body
   {:mst/message (->c-string message 64)}})

(defn mtc [message]
  {::packet/header
   {:size (+ 8 128) :type :mtc :request-info 0 :data 0}
   ::packet/body
   {:mtc/connection-id 255
    :mtc/player-id 0
    :spare2 0
    :spare3 0
    :mtc/message (->c-string message 128)}})
