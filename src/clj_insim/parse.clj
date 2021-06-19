(ns clj-insim.parse
  (:require [clj-insim.enum :as enum]
            [clj-insim.flags :as flags]
            [clj-insim.utils :as u]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private parser functions

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
  {:small enum/SMALL_HEADER_DATA
   :tiny enum/TINY_HEADER_DATA
   :ttc enum/TTC_HEADER_DATA})

(def ^:private INFO_BODY_PARSERS
  {:cch #:body{:camera (enum/decode enum/VIEW_IDENTIFIERS)}
   :cnl #:body{:reason (enum/decode enum/LEAVE_REASONS)}
   :ism #:body{:host (enum/decode enum/HOST)}
   :mso #:body{:user-type (enum/decode enum/USER_TYPE)}
   :ncn #:body{:admin #(when (= 1 %) :admin)
               :flags (enum/decode enum/PLAYER_TYPE)}
   :npl #:body{:player-type (enum/decode enum/PLAYER_TYPE)
               :flags (flags/parse flags/PLAYER)
               :tyres parse-tyres
               :setup-flags (flags/parse flags/SETUP_FLAGS)}
   :rst #:body{:race-laps #(if (zero? %) :qualifying %)
               :qualify-minutes #(if (zero? %) :race %)
               :wind (enum/decode enum/WIND)
               ;; TODO: Next step is to move flags data to flags ns
               :flags (flags/parse flags/RST)}
   ;; Some of the small packets can be received as info, and need parsing
   :small
   {:alc #:body{:cars (flags/parse flags/CARS)}
    :vta #:body{:action (enum/decode enum/ACTION)}}
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
  (let [data-enum (get HEADER_DATA type)]
    (cond-> header
      data-enum (update :header/data (enum/decode data-enum)))))

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
        (update :header/type (enum/decode enum/HEADER_TYPE))
        (parse-header-data))))

(defn body
  "Returns packet with it's :body/keys parsed, based on INFO_BODY_PARSERS
   ```clojure
  (body {:header/type :small :header/data :vta :body/unique-connection-id 1 :body/action 1})
  => {:header/type 4 :body/unique-connection-id 1 :body/action :end}```"
  [{:header/keys [type data] :as packet}]
  (let [parsers (if (#{:small} type)
                  (get-in INFO_BODY_PARSERS [type data] {})
                  (get INFO_BODY_PARSERS type {}))]
    (u/map-kv parsers (dissoc packet :body/spare))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parse clj-insim packets to raw instruction. This must be done prior to sending
;; the packet to LFS.

(def ^:private INSTRUCTION_BODY_PARSERS
  {:isi #:body{:admin #(u/c-str % 16) :iname #(u/c-str % 16) :prefix int}
   :mst #:body{:message #(u/c-str % 64)}
   :mtc #:body{:text #(u/c-str % (count %))}
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
    (cond-> (update header :header/type (enum/encode enum/HEADER_TYPE))
      data-enum (update :header/data (enum/encode data-enum)))))

(def instruction (comp parse-instruction-header parse-instruction-body))
