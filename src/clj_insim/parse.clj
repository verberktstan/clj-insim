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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private parsing data

(def ^:private HEADER_DATA
  {:small enum/SMALL_HEADER_DATA
   :tiny enum/TINY_HEADER_DATA
   :ttc enum/TTC_HEADER_DATA})

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

(def ^:private INFO_BODY_PARSERS
  {:cch #:body{:camera (enum/decode enum/VIEW_IDENTIFIERS)}
   :ism #:body{:host (enum/decode enum/HOST)}
   :mso #:body{:user-type (enum/decode enum/USER_TYPE)}
   :rst #:body{:race-laps #(if (zero? %) :qualifying %)
               :qualify-minutes #(if (zero? %) :race %)
               :wind (enum/decode enum/WIND)
               ;; TODO: Next step is to move flags data to flags ns
               :flags (partial flags/parse [:can-vote :can-select :mid-race :must-pit :can-reset :fcv :cruise])}
   :small
   {:alc #:body{:cars (partial flags/parse ALC_CARS)}
    :lcs #:body{:switches (partial flags/parse LCS_SWITCHES)}
    :tms #:body{:stop (enum/decode enum/STOP)}
    :vta #:body{:action (enum/decode enum/ACTION)}}
   :sta
   #:body{:flags (partial flags/parse STA_FLAGS)
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
   :sfp #:body{:flag (enum/encode SFP_FLAGS) :on-off (enum/encode [:off :on])}
   :sta #:body{:flags (partial flags/unparse STA_FLAGS)}})

(defn- parse-instruction-body [{:header/keys [type] :as packet}]
  (u/map-kv (INSTRUCTION_BODY_PARSERS type {}) packet))

(defn- parse-instruction-header [{:header/keys [type] :as header}]
  (let [data-enum (get HEADER_DATA type)]
    (cond-> (update header :header/type (enum/encode enum/HEADER_TYPE))
      data-enum (update :header/data (enum/encode data-enum)))))

(def instruction (comp parse-instruction-header parse-instruction-body))
