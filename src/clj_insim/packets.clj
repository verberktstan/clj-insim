(ns clj-insim.packets
  "clj-insim.packets provides functions that return maps that represent InSim
   packets. These are to be consumed by the parser (clj-insim.parse) and writer
   (clj-insim.write)"
  (:require [clj-insim.flags :as flags]
            [clj-insim.utils :as u]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Notice that :body/spare always gets a string. If the codec for a given
;; :body/spare is (m/ascii-string 2), the default :body/spare value should always
;; be a string of length 2!

(defn axm [{:keys [ucid action flags object-infos]
            :or {ucid 0 action :add-objects
                 flags #{:file-end} object-infos []}}]
  (let [n (count object-infos)]
    (merge
     #:header{:size (+ 4 4 (* n 8)) :type :axm :request-info 0 :data n}
     #:body{:ucid ucid :action action :flags flags :spare "0" :object-infos object-infos})))

(defn btn [{:keys [text request-info ucid click-id inst button-style type-in left top width height]
            :or {text "Hello button"
                 request-info 1
                 ucid 0
                 click-id 0
                 inst 0
                 button-style #{:click}
                 type-in 0
                 left (- 100 25)
                 top (- 100 5)
                 width 50
                 height 10}}]
  (let [clipped (u/clip-str text 240 4)]
    (merge
     #:header{:size (-> clipped count (+ 4 8)) :type :btn :request-info request-info :data ucid}
     #:body{:click-id click-id
            :inst inst
            :button-style button-style
            :type-in type-in
            :left left
            :top top
            :width width
            :height height
            :text clipped})))

(defn hcp
  "car-handicaps is expected to be a map with handicaps per car, like:
  `{:XFG {:car-handicap/mass 10 :car-handicap/restriction 10}}`"
  [car-handicaps]
  (merge
   #:header{:size 68 :type :hcp :request-info 0 :data 0}
   #:body{:car-handicaps ((apply juxt (map keyword flags/CARS)) car-handicaps)}))

;; InSimInit packet
(defn isi
  ([]
   (isi nil))
  ([{:keys [admin flags iname insim-version interval prefix]
      :or {admin "pwd"
           flags #{:con :hlv} ; Contact & Hot lap validity
           iname "clj-insim"
           insim-version 9
           interval 100
           prefix \!}}]
   ;; TODO: Implement is-flags
   (merge
    #:header{:size 44 :type :isi :request-info 1 :data 0}
    #:body{:udp-port 0 :flags flags :insim-version insim-version :prefix prefix
           :interval interval :admin admin :iname iname})))

(defn mtc [{:keys [ucid player-id text] :or {ucid 0 player-id 0 text "hello"}}]
  (let [clipped (u/clip-str text 128 4)]
    (merge
     #:header{:size (-> clipped count (+ 4 4)) :type :mtc :request-info 0 :data 0}
     #:body{:ucid ucid :player-id player-id :spare "00" :text clipped})))

(defn msl [{:keys [message sound]
            :or {message "Hello world" sound :silent}}]
  (merge
   #:header{:size 132 :type :msl :request-info 0 :data sound}
   #:body{:message message}))

(defn mst [{:keys [message] :or {message "Hello world"}}]
  (merge
   #:header{:size 68 :type :mst :request-info 0 :data 0}
   #:body{:message message}))

(defn msx [{:keys [message] :or {message "Hello world"}}]
  (merge
   #:header{:size 100 :type :msx :request-info 0 :data 0}
   #:body{:message message}))

;; TODO - Shouldn't :cars be a set of strings, like so; #{"XFG" "XRG"}
(defn plc [{:keys [cars ucid] :or {ucid 0 cars #{"XFG XRG"}}}]
  (merge
   #:header{:size 12 :type :plc :request-info 0 :data 0}
   #:body{:ucid ucid :spare "000" :cars cars}))

(defn reo [{:keys [player-ids] :or {player-ids [1 2 3]}}]
  (let [[_ zeroes] (split-at (count player-ids) (repeat 40 0))]
    (merge
     #:header{:size 44 :type :reo :request-info 0 :data 0}
     #:body{:player-ids (concat player-ids zeroes)})))

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
