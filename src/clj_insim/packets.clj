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
   {:header/size 44 :header/type :isi :header/request-info 1 :header/data 0
    :body/udp-port 0 :body/is-flags 0 :body/insim-version insim-version
    :body/prefix prefix :body/interval 0 :body/admin admin :body/iname iname}))

(defn tiny
  ([]
   (tiny nil))
  ([{:keys [request-info data]
      :or {request-info 0
           data :none}}]
   {:header/size 4 :header/type :tiny :header/request-info request-info :header/data data}))
