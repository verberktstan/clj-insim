(ns clj-insim.parsers
  (:require [clj-insim.enums :as enums]
            [clj-insim.util :refer [strip-null-chars]]))

(defn- byte-checked?
  "Returns true if nth-byte is checked.
  (byte-checked? 9 8) => true
  (byte-checked? 9 1) => true
  (byte-checked? 9 2) => false
  (byte-checked? 9 4) => false"
  [x nth-byte]
  (>= (rem x (* 2 nth-byte)) nth-byte))

(defn- parse-byte-flags [protocol x]
  (reduce-kv (fn [m k v]
               (assoc m k (byte-checked? x v))) {} protocol))

(defn- ->setup-flags [x]
  (when (< x (* 2 4))
    (parse-byte-flags
     {:symm-wheels 1 :tc-enable 2 :abs-enable 4} x)))

(defn- ->player-flags [x]
  (when (< x (* 2 16384))
    (parse-byte-flags
     {:swapside 1
      :reserved-2 2
      :reserved-4 4
      :autogears 8
      :shifter 16
      :reserved-32 32
      :help-b 64
      :axis-clutch 128
      :inpits 256
      :autoclutch 512
      :mouse 1024
      :kb-no-help 2048
      :kb-stabilised 4096
      :custom-view 8192} x)))

(defn- ->confirmation-flags [x]
  (when (< x (* 2 64))
    (let [flags (parse-byte-flags
                 {:mentioned 1
                  :confirmed 2
                  :penalty-drive-trough 4
                  :penalty-stop-go 8
                  :penalty-30 16
                  :penalty-45 32
                  :did-not-pit 64} x)]
      (cond
        (reduce #(or %1 %2) (vals (select-keys flags [:penalty-drive-trough :penalty-stop-go :did-not-pit])))
        (assoc flags :disqualified true)

        (reduce #(or %1 %2) (vals (select-keys flags [:penalty-30 :penalty-45])))
        (assoc flags :time true)

        :else flags))))

(defn- ->state-flags [x]
  (when (< x ( * 2 32768))
    (parse-byte-flags
     {:game 1 :replay 2 :paused 4 :shiftu 8
      :dialog 16 :shiftu-follow 32 :shiftu-no-opt 64 :show-2d 128
      :front-end 256 :multi 512 :mpspeedup 1024 :windowed 2048
      :sound-mute 4096 :view-override 8192 :visible 16384 :text-entry 32768} x)))

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
(defn- bytes->penalty [c]
  (-> c first enums/penalty-key))
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
(defn- bytes->tyre-compounds [c]
  (map enums/tyre-compounds-key c))
(defn- bytes->setup-flags [c]
  (-> c first ->setup-flags))
(defn- bytes->player-flags [[a b]]
  (->player-flags (+ (bit-shift-left b 8) a)))
(defn- bytes->confirmation-flags [c]
  (-> c first ->confirmation-flags))
(defn- bytes->unsigned [[a b c d]]
  (+ (bit-shift-left d 24) (bit-shift-left c 16) (bit-shift-left b 8) a))
(defn- bytes->word [[a b]]
  (+ (bit-shift-left b 8) a))
(defn- bytes->node-lap [result [node-a node-b lap-a lap-b player-id position]]
  (conj result {:node [node-a node-b] :lap [lap-a lap-b] :player-id player-id :position position}))

(defmulti ->byte-protocol type)
(defmethod ->byte-protocol clojure.lang.PersistentArrayMap [{:keys [key type length]}]
  (case type
    :confirmation-flags {:bytes 1 :cast bytes->confirmation-flags :key type}
    :cch-camera {:bytes 1 :cast bytes->cch-camera :key key}
    :csc-action {:bytes 1 :cast bytes->csc-action :key key}
    :flg-flag {:bytes 1 :cast bytes->flg-flag :key key}
    :penalty {:bytes 1 :cast bytes->penalty :key key}
    :pen-reason {:bytes 1 :cast bytes->pen-reason :key key}
    :reo-player-ids {:bytes length :cast #(into [] (map int %)) :key key}
    :string {:bytes length :cast bytes->string :key key}
    :type {:bytes 1 :cast bytes->isp-type :key key}
    :tiny-subtype {:bytes 1 :cast bytes->tiny-subtype :key key}
    :bytes {:bytes length :cast #(map int %) :key key}
    :int {:bytes 4 :cast #(map int %) :key key}
    :mso-user {:bytes 1 :cast bytes->mso-user :key key}
    :npl-player-type {:bytes 1 :cast bytes->npl-player-type :key key}
    :npl-tyres {:bytes 4 :cast bytes->tyre-compounds :key :tyres}
    :npl-setup-flags {:bytes 1 :cast bytes->setup-flags :key :setup-flags}
    :player-flags {:bytes 2 :cast bytes->player-flags :key :flags}
    :sta-race-in-progress {:bytes 1 :cast bytes->sta-race-in-progress :key key}
    :vtn-action {:bytes 1 :cast bytes->vtn-action :key key}
    :unsigned {:bytes 4 :cast bytes->unsigned :key key}
    :state-flags {:bytes 2 :cast #(-> % bytes->word ->state-flags) :key key}
    :float {:bytes 4 :cast #(fn [x] nil) :key key}
    :word {:bytes 2 :cast bytes->word :key key}
    :node-laps {:bytes (* 6 40) :cast #(reduce bytes->node-lap [] (partition 6 %)) :key key}
    {:bytes 1 :cast bytes->int :key key}))

(defmethod ->byte-protocol clojure.lang.Keyword [k]
  (let [base-map (case k
                   :type {:type :type}
                   :csc-action {:type k}
                   {})]
    (->byte-protocol (assoc base-map :key k))))

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

   ;; IS_FIN - FINished race
   :fin [:type :reqi :player-id
         {:key :race-time :type :unsigned}
         {:key :best-lap :type :unsigned}
         :spare-a :num-stops
         {:type :confirmation-flags}
         :spare-b
         {:key :laps-done :type :word}
         {:key :flags :type :word}]

   ;; IS_LAP - LAP
   :lap [:type :reqi :player-id
         {:key :lap-time :type :unsigned}
         {:key :total-time :type :unsigned}
         {:key :laps-done :type :word}
         {:key :flags :type :word}
         :spare-0 
         {:key :penalty :type :penalty}
         :num-stops :spare-3]

   ;; IS_NLP - Node and Lap Packet
   :nlp [:type :reqi :num-players
         {:key :players :type :node-laps}]

   ;; IS_NPL - New PLayer
   :npl [:type :reqi :player-id :uniq-connection-id
         {:key :player-type :type :npl-player-type}
         {:type :player-flags}
         {:key :player-name :type :string :length 24}
         {:key :license-plate :type :string :length 8}
         {:key :car-name :type :string :length 4}
         {:key :skin-name :type :string :length 16}
         {:type :npl-tyres}
         :handicap-mass :handicap-restriction :driver-model :passenger
         {:key :spare :type :int}
         {:type :npl-setup-flags}
         :number-player :spare-2 :spare-3]

   ;; IS_PEN - PENalty (given or cleared)
   :pen [:type :reqi :player-id
         {:key :old-penalty :type :penalty}
         {:key :new-penalty :type :penalty}
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
         :spare-a :num-stops
         {:type :confirmation-flags}
         :spare-b
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
         :split
         {:key :penalty :type :penalty}
         :num-stops :spare-3]

   ;; IS_STA
   :sta [:type :reqi :zero
         {:key :replay-speed :type :float}
         {:key :flags :type :state-flags}
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

;;;;; Public functions

(defn make-protocol [k]
  (let [c (is-protocols k)]
    (when c
      (into [] (map ->byte-protocol c)))))

(defn parse-bytes
  [{:keys [coll] :as m} {:keys [bytes cast key]}]
  (let [[c1 c2] (split-at bytes coll)]
    (if (empty? c2)
      (-> m ; When nothing left to parse..
          (dissoc :coll) ; Dissociate coll
          (assoc key (cast c1)))
      (-> m
          (assoc :coll c2) ; else replace coll
          (assoc key (cast c1))))))
