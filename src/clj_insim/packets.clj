(ns clj-insim.packets)

;; InSimInit packet
(defn isi
  ([]
   (isi nil))
  ([{:keys [admin iname insim-version prefix]
      :or {admin "pwd"
           iname "clj-insim"
           insim-version 8
           prefix \!}}]
   (merge
    #:header{:size 44 :type :isi :request-info 1 :data 0}
    #:body{:udp-port 0 :is-flags 0 :insim-version insim-version :prefix prefix
           :interval 0 :admin admin :iname iname})))

(defn tiny
  ([]
   (tiny nil))
  ([{:keys [request-info data]
      :or {request-info 0
           data :none}}]
   #:header{:size 4 :type :tiny :request-info request-info :data data}))
