(ns clj-insim.parse
  (:require [clj-insim.enum :as enum]
            [clj-insim.flags :as flags]
            [clj-insim.utils :as u]
            [clojure.set :as set]))

(defn- raw?
  "Returns `true` if header/type is an integer value. This means that this
   packet's header is a raw packet, received from LFS or prepared instruction
   for sending to LFS."
  [{:header/keys [type]}]
  (int? type))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private parser functions

(defn- parse-car-contact [{:car-contact/keys [throttle-brake] :as car-contact}]
  (let [throttle (unsigned-bit-shift-right throttle-brake 4)
        brake (bit-shift-left throttle-brake 4)]
    (-> car-contact
        (assoc :car-contact/throttle throttle)
        (assoc :car-contact/brake brake))))

(defn- parse-race-laps [rl]
  (cond
    (zero? rl) {:practice true}
    (< rl 100) {:laps rl}
    (< rl 191) {:laps (-> rl (- 100) (* 10) (+ 100))}
    (> rl 190) {:hours (- rl 190)}))

(defn- parse-tyres [tyres]
  (let [f (enum/decode enum/COMPOUNDS)]
    (->> tyres
      (map #(vector %1 (f %2)) [:rear-left :rear-right :front-left :front-right])
      (into {}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private parsing data

(def ^:private HEADER_DATA
  {:bfn enum/BFN_HEADER_DATA
   :msl enum/MESSAGE_SOUNDS
   :small enum/SMALL_HEADER_DATA
   :tiny enum/TINY_HEADER_DATA
   :ttc enum/TTC_HEADER_DATA})

(def ^:private data->player-id
  {:header/data :header/player-id})

;; In some cases, the header/data key must be renamed so we provide rename-keys
;; kmaps per packet type here.
(def ^:private HEADER_RENAMES
  {:axi data->player-id
   :btc {:header/data :header/ucid}
   :crs data->player-id
   :hlv data->player-id
   :pll data->player-id
   :plp data->player-id})

(def ^:private INFO_BODY_PARSERS
  {:axm #:body{:action (enum/decode enum/PMO_ACTION)
               :flags (flags/parse flags/PMO)}
   :btc #:body{:flags (flags/parse [:lmb :rmb :ctrl :shift])}
   :cch #:body{:camera (enum/decode enum/VIEW_IDENTIFIERS)}
   :cnl #:body{:reason (enum/decode enum/LEAVE_REASONS)}
   ;; TODO add parsing for the CON packet
   :fin #:body{:confirm (flags/parse flags/CONFIRMATION)
               :flags (flags/parse flags/PLAYER)}
   :flg #:body{:off-on (enum/decode [:off :on])
               :flag (flags/parse [:given-blue :causing-yellow])}
   :hlv #:body{:hlvc (enum/decode [:ground :wall nil :speeding :out-of-bounds])}
   :ism #:body{:host (enum/decode enum/HOST)}
   :lap #:body{:flags (flags/parse flags/PLAYER)
               :penalty (enum/decode enum/PENALTY)}
   :mso #:body{:user-type (enum/decode enum/USER_TYPE)}
   :ncn #:body{:admin #(when (= 1 %) :admin)
               :flags (enum/decode enum/PLAYER_TYPE)}
   :npl #:body{:player-type (enum/decode enum/PLAYER_TYPE)
               :flags (flags/parse flags/PLAYER)
               :tyres parse-tyres
               :setup-flags (flags/parse flags/SETUP)}
   ;; TODO add parsing for the car-contact data in OBH
   :obh #:body{:flags (flags/parse flags/OBH)}
   :pen #:body{:old-penalty (enum/decode enum/PENALTY)
               :new-penalty (enum/decode enum/PENALTY)
               :reason (enum/decode enum/PENALTY_REASONS)}
   :pfl #:body{:flags (flags/parse flags/PLAYER)}
   :pit #:body{:flags (flags/parse flags/PLAYER)
               :penalty (enum/decode enum/PENALTY)
               :tyres parse-tyres
               :pit-work (flags/parse flags/PIT_WORK)}
   :pla #:body{:fact (enum/decode enum/PIT_LANE_FACTS)}
   :res #:body{:confirmation-flags (flags/parse flags/CONFIRMATION)
               :flags (flags/parse flags/PLAYER)}
   :rst #:body{:race-laps #(if (zero? %) :qualifying %)
               :qualify-minutes #(if (zero? %) :race %)
               :wind (enum/decode enum/WIND)
               :flags (flags/parse flags/RST)}
   ;; Some of the small packets can be received as info, and need parsing
   :small
   {:alc #:body{:cars (flags/parse flags/CARS)}
    :vta #:body{:action (enum/decode enum/ACTION)}}
   :spx #:body{:penalty (enum/decode enum/PENALTY)}
   :sta
   #:body{:flags (flags/parse flags/STA)
          :in-game-cam (enum/decode enum/VIEW_IDENTIFIERS)
          :race-in-progress (enum/decode enum/RACE_IN_PROGRESS)
          :race-laps parse-race-laps
          :wind (enum/decode enum/WIND)}
   :vtn #:body{:action (enum/decode enum/ACTION)}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private parse functions

(defn- parse-header-data
  "Returns header with `:header/data` parsed.
   `(parse-data #:header{:type :tiny :data 2}) => #:header{:type :tiny :data :close}`"
  [{:header/keys [type] :as header}]
  (let [data-enum (get HEADER_DATA type)
        rename-kmap (get HEADER_RENAMES type)]
    (cond-> header
      data-enum (update :header/data (enum/decode data-enum))
      rename-kmap (set/rename-keys rename-kmap))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parse raw info packets to clj-insim packets. This must be done - immediately -
;; when we receive a packet from LFS.

(defn header
  "Returns header with `:header/type` and `:header/data` parsed. Returns the input
   header when it is not a 'raw' header.
   `(header #:header{:type 3 :data 2}) => #:header{:type :tiny :data :close}`"
  [header]
  (cond-> header
    (raw? header) (update :header/type (enum/decode enum/HEADER_TYPE))
    (raw? header) (parse-header-data)))

(defn body
  "Returns packet with it's :body/keys parsed, based on INFO_BODY_PARSERS
   ```clojure
  (body {:header/type :small :header/data :vta :body/action 1}) =>
  {:header/type :small :header/data :vta :body/action :end}```"
  [{:header/keys [type data] :as packet}]
  (let [parsers (if (#{:small} type)
                  (get-in INFO_BODY_PARSERS [type data] {})
                  (get INFO_BODY_PARSERS type {}))]
    (u/map-kv parsers packet)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parse clj-insim packets to raw instruction. This must be done prior to sending
;; the packet to LFS.

(def ^:private INSTRUCTION_BODY_PARSERS
  {:axm #:body{:action (enum/encode enum/PMO_ACTION)
               :flags (flags/unparse flags/PMO)}
   :btn #:body{:button-style (flags/unparse flags/BUTTON_STYLE)}
   :isi #:body{:admin #(u/c-str % 16)
               :flags (flags/unparse flags/ISI)
               :iname #(u/c-str % 16)
               :prefix int}
   :msl #:body{:message #(u/c-str % 128)}
   :mst #:body{:message #(u/c-str % 64)}
   :msx #:body{:message #(u/c-str % 96)}
   :mtc #:body{:text #(u/c-str % (count %))}
   :plc #:body{:cars (flags/unparse flags/CARS)}
   :scc #:body{:in-game-cam (enum/encode enum/VIEW_IDENTIFIERS)}
   :sch #:body{:char int :flag (enum/encode [:shift :ctrl])}
   :sfp #:body{:flag (enum/encode enum/SFP) :on-off (enum/encode [:off :on])}
   ;; Some of the small packets can be send as instructions and need parsing..
   :small
   {:alc #:body{:cars (flags/unparse flags/CARS)}
    :lcs #:body{:switches (flags/unparse flags/SWITCHES)}
    :tms #:body{:stop (enum/encode enum/STOP)}}})

(defn- parse-instruction-body [{:header/keys [type data] :as packet}]
  (let [parsers (if (#{:small} type)
                  (get-in INSTRUCTION_BODY_PARSERS [type data] {})
                  (get INSTRUCTION_BODY_PARSERS type {}))]
    (u/map-kv parsers packet)))

(defn- parse-instruction-header [{:header/keys [type] :as header}]
  (let [data-enum (get HEADER_DATA type)]
    (cond-> header
      (not (raw? header)) (update :header/type (enum/encode enum/HEADER_TYPE))
      (and (not (raw? header)) data-enum) (update :header/data (enum/encode data-enum)))))

(def instruction (comp parse-instruction-header parse-instruction-body))
