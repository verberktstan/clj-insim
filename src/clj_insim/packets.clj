(ns clj-insim.packets)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Notice that :body/spare always gets a string. If the codec for a given
;; :body/spare is (m/ascii-string 2), the default :body/spare value should always
;; be a string of length 2!

;; InSimInit packet
(defn isi
  ([]
   (isi nil))
  ([{:keys [admin iname insim-version prefix]
      :or {admin "pwd"
           iname "clj-insim"
           insim-version 8
           prefix \!}}]
   ;; TODO: Implement is-flags
   (merge
    #:header{:size 44 :type :isi :request-info 1 :data 0}
    #:body{:udp-port 0 :is-flags 0 :insim-version insim-version :prefix prefix
           :interval 0 :admin admin :iname iname})))

(defn scc [{:keys [player-id in-game-cam] :or {player-id 0 in-game-cam :driver}}]
  (merge
   #:header{:size 8 :type :scc :request-info 0 :data 0}
   #:body{:player-id player-id :in-game-cam in-game-cam :spare "00"}))

(defn sch [{:keys [char flag] :or {char \A flag :shift}}]
  (merge
   #:header{:size 8 :type :sch :request-info 0 :data 0}
   #:body{:char char :flag flag :spare "00"}))

(defn sfp
  [{:keys [flag on-off] :or {flag :shift-u-no-opt on-off :on}}]
  (merge
   #:header{:size 8 :type :sfp :request-info 0 :data 0}
   #:body{:flag flag :on-off on-off :spare "0"}))

(defn tiny
  ([]
   (tiny nil))
  ([{:keys [request-info data]
      :or {request-info 0
           data :none}}]
   #:header{:size 4 :type :tiny :request-info request-info :data data}))
