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

(defn- parse-protocol-map [{:keys [key type length]}]
  (case type
    :string {:bytes length :cast bytes->string :key key}
    :type {:bytes 1 :cast bytes->isp-type :key key}
    :tiny-subtype {:bytes 1 :cast bytes->tiny-subtype :key key}
    :word {:bytes 2 :cast #(map int %) :key key}
    :bytes {:bytes length :cast #(map int %) :key key}
    :int {:bytes 4 :cast #(map int %) :key key}
    :float {:bytes 4 :cast #(map int %) :key key}
    {:bytes 1 :cast bytes->int :key key}))

(defn- make-protocol [c]
  (into [] (map parse-protocol-map c)))

(def ^{:private true} is-protocols
  {:csc (make-protocol
         [{:key :type :type :type} {:key :reqi} {:key :player-id}
          {:key :spare} {:key :csc-action} {:key :spare-2} {:key :spare-3}])
   :flg (make-protocol
         [{:key :type :type :type} {:key :reqi} {:key :player-id}
          {:key :off-on} {:key :flag} {:key :car-behind} {:key :spare-3}])
   :ism (make-protocol
         [{:key :type :type :type} {:key :reqi} {:key :zero}
          {:key :host} {:key :spare-1} {:key :spare-2} {:key :spare-3}
          {:key :host-name :type :string :length 32}])
   :mso (make-protocol
         [;{:key :size}
          {:key :type :type :type}
          {:key :reqi}
          {:key :zero}
          {:key :uniq-connection-id}
          {:key :player-id}
          {:key :user-type}
          {:key :text-start}
          {:key :message :type :string :length 128}])
   :npl (make-protocol
         [;{:key :size}
          {:key :type :type :type}
          {:key :reqi}
          {:key :player-id}
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
          {:key :spare-3}])
   :tiny (make-protocol
          [;{:key :size}
           {:key :type :type :type}
           {:key :reqi}
           {:key :subt-type :type :tiny-subtype}])
   :sta (make-protocol
         [{:key :type :type :type} {:key :reqi} {:key :zero}
          {:key :replay-speed :type :float} {:key :flags :type :word}
          {:key :ingame-cam} {:key :viewed-player-id}
          {:key :num-players-in-race} {:key :num-connections}
          {:key :num-finished} {:key :race-in-progress}
          {:key :qualify-minutes} {:key :race-laps}
          {:key :spare-2} {:key :spare-3}
          {:key :track :type :string :length 6}
          {:key :weather} {:key :wind}])
   :ver (make-protocol
         [;{:key :size}
          {:key :type :type :type} ; The first byte is parsed as {:type :some-key}
          {:key :reqi} ; The second byte is parsed as {:reqi int}
          {:key :zero}
          {:key :version :type :string :length 8} ; The next 8 bytes are parsed as {:version "string"}
          {:key :product :type :string :length 6} ; The next 6 bytes are parsed as {:product "string"}
          {:key :insim-version} ; {:insim-version int}
          {:key :spare}])})


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
  (let [protocol (is-protocols type-key)]
    (when protocol
      (reduce parse-bytes {:coll packet} protocol))))
