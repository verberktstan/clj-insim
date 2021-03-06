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
         {:udp-port 0 :is-flags #{} :insim-version 8 :prefix (int \!) :interval 0
          :admin "pwd"
          :iname "clj-insim"}
         params)
        (update :admin ->c-string 16)
        (update :iname ->c-string 16))}))

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
   {:message (->c-string message 64)}})

(defn mtc
  ([message]
   (mtc 255 message))
  ([ucid message]
   {::packet/header
    {:size (+ 8 128) :type :mtc :request-info 0 :data 0}
    ::packet/body
    {:connection-id ucid
     :player-id 0
     :spare2 0
     :spare3 0
     :message (->c-string message 128)}}))

(defn jrr [ucid action]
  {::packet/header
   {:size 16 :type :jrr :request-info 0 :data 0}
   ::packet/body
   {:ucid ucid
    :jrr-action (get {:allow 1} action 0)
    :spare2 0
    :spare3 0

    :x 0, :y 0, :z-byte 0, :flags 0, :index 0 :heading 0}})

(defn plc [ucid cars]
  {::packet/header
   {:size 12 :type :plc :request-info 0 :data 0}
   ::packet/body
   {:ucid ucid :spare1 0 :spare2 0 :spare3 0
    :cars cars}})
