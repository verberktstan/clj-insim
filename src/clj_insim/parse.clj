(ns clj-insim.parse
  (:require [clj-insim.flags :as flags]
            [clj-insim.utils :as u]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private parser functions

(defn- parse-race-laps [rl]
  (cond
    (zero? rl) {:practice true}
    (< rl 100) {:laps rl}
    (< rl 191) {:laps (-> rl (- 100) (* 10) (+ 100))}
    (> rl 190) {:hours (- rl 190)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private parsing data

(def ^:private TYPES
  [:none :isi :ver :tiny :small :sta :sch :sfp :scc :cpp :ism :mso :iii :mst :mtc
   :mod :vtn :rst :ncn :cnl :cpr :npl :plp :pll :lap :spx :pit :psf :pla :cch
   :pen :toc :flg :pfl :fin :res :reo :nlp :mci :msx :msl :crs :bfn :axi :axo
   :btn :btc :btt :rip :ssh :con :obh :hlv :plc :axm :acr :hcp :nci :jrr :uco :oco
   :ttc :slc :csc :cim])

(def ^:private DATA
  {:small
   [:none :ssp :ssg :vta :tms :stp :rtp :nli :alc :lcs]

   :tiny
   [:none :ver :close :ping :reply :vtc :scp :sst :gth :mpe :ism :ren :clr :ncn
    :npl :res :nlp :mci :reo :rst :axi :axc :rip :nci :alc :axm :slc]

   :ttc
   [:none :sel :sel-start :sel-stop]})

(def ^:private SFP_FLAGS
  [:shift-u-no-opt :show-2d :mspeedup :sound-mute])

(def ^:private STA_FLAGS
  [:game :replay :paused :shift-u :dialog :shift-u-follow :shift-u-no-opt
   :show-2d :front-end :multi :mspeedup :windowed :sound-mute :view-override
   :visible :text-entry])

(def ^:private ALC_CARS
  ["XFG" "XRG" "XRT" "RB4" "FXO" "LX4" "LX6" "MRT" "UF1" "RAC" "FZ5" "FOX" "XFR" "UFR" "FO8" "FXR" "XRR" "FZR" "BF1" "FBM"])

(def ^:private LCS_SWITCHES
  [:set-signals :set-flash :headlights :horn :siren])

(def ^:private VIEW_IDENTIFIERS
  [:follow :heli :cam :driver :custom])

(def ^:private INFO_BODY_PARSERS
  {:cch  #:body{:camera (partial nth VIEW_IDENTIFIERS)}
   :small
   {:alc #:body{:cars (partial flags/parse ALC_CARS)}
    :lcs #:body{:switches (partial flags/parse LCS_SWITCHES)}
    :tms #:body{:stop (partial nth [:carry-on :stop])}
    :vta #:body{:action (partial nth [:none :end :restart :qualify])}}
   :sta
   #:body{:flags (partial flags/parse STA_FLAGS)
          :in-game-cam (partial nth VIEW_IDENTIFIERS)
          :race-in-progress (partial nth [:no-race :race :qualifying])
          :race-laps parse-race-laps
          :wind (partial nth [:off :weak :strong])}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private parse functions

(defn- parse-header-data
  "Returns header with `:header/data` parsed.
   `(parse-data #:header{:type :tiny :data 2}) => #:header{:type :tiny :data :close}`"
  [{:header/keys [type] :as header}]
  (let [data-enum (get DATA type)]
    (cond-> header
      data-enum (update :header/data (partial nth data-enum)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parse raw info packets to clj-insim packets. This must be done - immediately -
;; when we receive a packet from LFS.

(defn header
  "Returns header with `:header/type` and `:header/data` parsed. Returns `nil`
   when header or type are falsey.
   `(header #:header{:type 3 :data 2}) => #:header{:type :tiny :data :close}`"
  [{:header/keys [type] :as header}]
  (when (and header type)
    (-> header
        (update :header/type (partial nth TYPES))
        (parse-header-data))))

(defn body
  "Returns packet with it's :body/keys parsed, based on INFO_BODY_PARSERS
   ```clojure
  (body {:header/type :small :header/data :vta :body/unique-connection-id 1 :body/action 1})
  => {:header/type 4 :body/unique-connection-id 1 :body/action :end}```"
  [{:header/keys [type data] :as packet}]
  (let [parsers (if-let [parsers (and (#{:small} type)
                                      (or (get-in INFO_BODY_PARSERS [type data]) {}))]
                  parsers
                  (INFO_BODY_PARSERS type {}))]
    (u/map-kv parsers (dissoc packet :body/spare))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parse clj-insim packets to raw instruction. This must be done prior to sending
;; the packet to LFS.

(def ^:private INSTRUCTION_BODY_PARSERS
  {:isi #:body{:admin #(u/c-str % 16) :iname #(u/c-str % 16) :prefix int}
   :sch #:body{:char int :flag (u/index-of [:shift :ctrl])}
   :sfp #:body{:flag (u/index-of SFP_FLAGS) :on-off (u/index-of [:off :on])}
   :sta #:body{:flags (partial flags/unparse STA_FLAGS)}})

(defn- parse-instruction-body [{:header/keys [type] :as packet}]
  (u/map-kv (INSTRUCTION_BODY_PARSERS type {}) packet))

(defn- parse-instruction-header [{:header/keys [type] :as header}]
  (let [data-enum (get DATA type)]
    (cond-> (update header :header/type (u/index-of TYPES))
      data-enum (update :header/data (u/index-of data-enum)))))

(def instruction (comp parse-instruction-header parse-instruction-body))
