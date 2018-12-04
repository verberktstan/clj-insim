(ns clj-insim.parsers
  (:require [clj-insim.enums :as enums]
            [clj-insim.util :refer [strip-null-chars]]))


(defn- bytes->int [c] (-> c first int))
(defn- bytes->string [c]
  (->>
   (map char (strip-null-chars c))
   (apply str)))
(defn- bytes->isp-type [c]
  (-> c first enums/isp-key))
(defn- bytes->tiny-subtype [c]
  (-> c first enums/tiny-key))
(defn- bytes->sta-race-in-progress [c]
  (-> c first enums/sta-race-in-progress-key))
(defn- bytes->vtn-action [c]
  (-> c first enums/vtn-action-key))

(defn- parse-protocol-map [{:keys [key type length]}]
  (case type
    :string {:bytes length :cast bytes->string :key key}
    :type {:bytes 1 :cast bytes->isp-type :key key}
    :tiny-subtype {:bytes 1 :cast bytes->tiny-subtype :key key}
    :word {:bytes 2 :cast #(map int %) :key key}
    :bytes {:bytes length :cast #(map int %) :key key}
    :int {:bytes 4 :cast #(map int %) :key key}
    :float {:bytes 4 :cast #(map int %) :key key}
    :sta-race-in-progress {:bytes 1 :cast bytes->sta-race-in-progress :key key}
    :vtn-action {:bytes 1 :cast bytes->vtn-action :key key}
    {:bytes 1 :cast bytes->int :key key}))

(defn- make-protocol [c]
  (when c
    (into [] (map parse-protocol-map c))))

(def ^{:private true} is-protocols
  {
   ;; IS_FLG
   :flg [{:key :type :type :type} {:key :reqi} {:key :player-id}
         {:key :off-on}
         {:key :flag}
         {:key :car-behind}
         {:key :spare-3}]

   ;; IS_ISM
   :ism [{:key :type :type :type} {:key :reqi} {:key :zero}
         {:key :host}
         {:key :spare-1}
         {:key :spare-2}
         {:key :spare-3}
         {:key :host-name :type :string :length 32}]

   ;; IS_MSO
   :mso [{:key :type :type :type} {:key :reqi} {:key :zero}
         {:key :uniq-connection-id}
         {:key :player-id}
         {:key :user-type}
         {:key :text-start}
         {:key :message :type :string :length 128}]

   ;; IS_NCN - New ConnectioN
   :ncn [{:key :type :type :type} {:key :reqi} {:key :uniq-connection-id}
         {:key :user-name :type :string :length 24}
         {:key :player-name :type :string :length 24}
         {:key :admin}
         {:key :total}
         {:key :flags}
         {:key :spare}]

   ;; IS_NPL
   :npl [{:key :type :type :type} {:key :reqi} {:key :player-id}
         {:key :uniq-connection-id}
         {:key :player-type}
         {:key :flags :type :word}
         {:key :player-name :type :string :length 24}
         {:key :license-plate :type :string :length 8}
         {:key :car-name :type :string :length 4}
         {:key :skin-name :type :string :length 16}
         {:key :tyres :type :bytes :length 4}
         {:key :handicap-mass}
         {:key :handicap-restriction}
         {:key :driver-model}
         {:key :passenger}
         {:key :spare :type :int}
         {:key :setup-flags}
         {:key :number-player}
         {:key :spare-2}
         {:key :spare-3}]

   ;; IS_TINY
   :tiny [{:key :type :type :type} {:key :reqi} {:key :subt-type :type :tiny-subtype}]

   ;; IS_STA
   :sta [{:key :type :type :type} {:key :reqi} {:key :zero}
         {:key :replay-speed :type :float}
         {:key :flags :type :word}
         {:key :ingame-cam}
         {:key :viewed-player-id}
         {:key :num-players-in-race}
         {:key :num-connections}
         {:key :num-finished}
         {:key :race-in-progress :type :sta-race-in-progress}
         {:key :qualify-minutes}
         {:key :race-laps}
         {:key :spare-2}
         {:key :spare-3}
         {:key :track :type :string :length 6}
         {:key :weather}
         {:key :wind}]

   ;; IS_VER
   :ver [{:key :type :type :type} {:key :reqi} {:key :zero}
         {:key :version :type :string :length 8}
         {:key :product :type :string :length 6}
         {:key :insim-version}
         {:key :spare}]

   ;; IS_VTN - VoTe Notify
   :vtn [{:key :type :type :type} {:key :reqi} {:key :zero}
         {:key :uniq-connection-id}
         {:key :action :type :vtn-action}
         {:key :spare-2}
         {:key :spare-3}]})


(defn- parse-bytes
  [{:keys [coll] :as m} {:keys [bytes cast key]}]
  (let [[c1 c2] (split-at bytes coll)]
    (if (empty? c2)
      (-> m ; When nothing left to parse..
          (dissoc :coll) ; Dissociate coll
          (assoc key (cast c1)))
      (-> m
          (assoc :coll c2) ; else replace coll
          (assoc key (cast c1))))))

(defn parse [type-key packet]
  (let [protocol (make-protocol (is-protocols type-key))]
    (when protocol
      (reduce parse-bytes {:coll packet} protocol))))
