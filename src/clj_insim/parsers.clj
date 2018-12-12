(ns clj-insim.parsers
  (:require [clj-insim.enums :as enums]
            [clj-insim.util :refer [strip-null-chars]]))


(defn- bytes->int [c] (-> c first int))
(defn- bytes->string [c]
  (->>
   (map char (strip-null-chars c))
   (apply str)))
(defn- bytes->cch-camera [c]
  (-> c first enums/cch-camera-key))
(defn- bytes->csc-action [c]
  (-> c first enums/csc-action-key))
(defn- bytes->flg-flag [c]
  (-> c first enums/flg-flag-key))
(defn- bytes->pen-penalty [c]
  (-> c first enums/pen-penalty-key))
(defn- bytes->pen-reason [c]
  (-> c first enums/pen-reason-key))
(defn- bytes->isp-type [c]
  (-> c first enums/isp-key))
(defn- bytes->tiny-subtype [c]
  (-> c first enums/tiny-key))
(defn- bytes->sta-race-in-progress [c]
  (-> c first enums/sta-race-in-progress-key))
(defn- bytes->vtn-action [c]
  (-> c first enums/vtn-action-key))
(defn- bytes->mso-user [c]
  (-> c first enums/mso-user-key))
(defn- bytes->npl-player-type [c]
  (-> c first enums/npl-player-type-key))

(defmulti ->byte-protocol type)
(defmethod ->byte-protocol clojure.lang.PersistentArrayMap [{:keys [key type length]}]
  (case type
    :cch-camera {:bytes 1 :cast bytes->cch-camera :key key}
    :csc-action {:bytes 1 :cast bytes->csc-action :key key}
    :flg-flag {:bytes 1 :cast bytes->flg-flag :key key}
    :pen-penalty {:bytes 1 :cast bytes->pen-penalty :key key}
    :pen-reason {:bytes 1 :cast bytes->pen-reason :key key}
    :reo-player-ids {:bytes length :cast #(into [] (map int %)) :key key}
    :string {:bytes length :cast bytes->string :key key}
    :type {:bytes 1 :cast bytes->isp-type :key key}
    :tiny-subtype {:bytes 1 :cast bytes->tiny-subtype :key key}
    :word {:bytes 2 :cast #(apply + %) :key key}
    :bytes {:bytes length :cast #(map int %) :key key}
    :int {:bytes 4 :cast #(map int %) :key key}
    :float {:bytes 4 :cast #(apply + %) :key key}
    :mso-user {:bytes 1 :cast bytes->mso-user :key key}
    :npl-player-type {:bytes 1 :cast bytes->npl-player-type :key key}
    :sta-race-in-progress {:bytes 1 :cast bytes->sta-race-in-progress :key key}
    :vtn-action {:bytes 1 :cast bytes->vtn-action :key key}
    :unsigned {:bytes 4 :cast #(map int %) :key key}
    {:bytes 1 :cast bytes->int :key key}))

(defmethod ->byte-protocol clojure.lang.Keyword [k]
  (let [base-map (case k
                   :type {:type :type}
                   :csc-action {:type k}
                   {})]
    (->byte-protocol (assoc base-map :key k))))

(defn- make-protocol [c]
  (when c
    (into [] (map ->byte-protocol c))))

(def ^{:private true} is-protocols
  {;; IS_CCH - Camera CHange
   :cch [:type :reqi :player-id
         {:key :camera :type :cch-camera}
         :spare-1 :spare-2 :spare-3]

   ;; IS_FLG
   :flg [:type :reqi :player-id :off-on
         {:key :flag :type :flg-flag}
         :car-behind :spare-3]

   ;; IS_ISM
   :ism [:type :reqi :zero :host :spare-1 :spare-2 :spare-3 
         {:key :host-name :type :string :length 32}]

   ;; IS_MSO
   :mso [:type :reqi :zero :uniq-connection-id :player-id
         {:key :user-type :type :mso-user}
         :text-start
         {:key :message :type :string :length 128}]

   ;; IS_NCN - New ConnectioN
   :ncn [:type :reqi :uniq-connection-id
         {:key :user-name :type :string :length 24}
         {:key :player-name :type :string :length 24}
          :admin :total :flags :spare]

   ;; IS_CNL - CoNnection Left
   :cnl [:type :reqi :uniq-connection-id :reason :total :spare-2 :spare-3]

   ;; IS_NPL - New PLayer
   :npl [:type :reqi :player-id :uniq-connection-id
         {:key :player-type :type :npl-player-type}
         {:key :flags :type :word}
         {:key :player-name :type :string :length 24}
         {:key :license-plate :type :string :length 8}
         {:key :car-name :type :string :length 4}
         {:key :skin-name :type :string :length 16}
         {:key :tyres :type :bytes :length 4}
         :handicap-mass :handicap-restriction :driver-model :passenger
         {:key :spare :type :int}
         :setup-flags :number-player :spare-2 :spare-3]

   ;; IS_PEN - PENalty (given or cleared)
   :pen [:type :reqi :player-id
         {:key :old-penalty :type :pen-penalty}
         {:key :new-penalty :type :pen-penalty}
         {:key :reason :type :pen-reason} :spare-3]

   ;; IS_PLL ; PLayer Leave race
   :pll [:type :reqi :player-id]

   ;; IS_RES ; RESult of qualify or confirmed finish
   :res [:type :reqi :player-id
         {:key :user-name :type :string :length 24}
         {:key :player-name :type :string :length 24}
         {:key :number-plate :type :string :length 8}
         {:key :skin-prefix :type :string :length 4}
         {:key :race-time :type :unsigned}
         {:key :best-lap :type :unsigned}
         :spare-a :num-stops :confirmation-flags :spare-b
         {:key :laps-done :type :word}
         {:key :flags :type :word}
          :result-num :num-results
         {:key :penalty-seconds :type :word}]

   ;; IS_TINY
   :tiny [:type :reqi {:key :subt-type :type :tiny-subtype}]

   ;; IS_REO - RE-Order
   :reo [:type :reqi :number-of-players
         {:key :player-ids :type :reo-player-ids :length 40}]

   ;; IS_SPX - SPlit X time
   :spx [:type :reqi :player-id
         {:key :split-time :type :unsigned}
         {:key :total-time :type :unsigned}
         :split :penalty :num-stops :spare-3]

   ;; IS_STA
   :sta [:type :reqi :zero
         {:key :replay-speed :type :float}
         {:key :flags :type :word}
         :ingame-cam :viewed-player-id :num-players-in-race :num-connections :num-finished
         {:key :race-in-progress :type :sta-race-in-progress}
         :qualify-minutes :race-laps :spare-2 :spare-3
         {:key :track :type :string :length 6}
         :weather :wind]

   ;; IS_VER
   :ver [:type :reqi :zero
         {:key :version :type :string :length 8}
         {:key :product :type :string :length 6}
         :insim-version :spare]

   ;; IS_VTN - VoTe Notify
   :vtn [:type :reqi :zero
         :uniq-connection-id
         {:key :action :type :vtn-action}
         :spare-2 :spare-3]})

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

(defn parse
  ([packet]
   (let [type-key (-> packet first enums/isp-key)]
     (parse type-key packet)))
  ([type-key packet]
   (let [protocol (make-protocol (is-protocols type-key))]
     (when protocol
       (reduce parse-bytes {:coll packet} protocol)))))
