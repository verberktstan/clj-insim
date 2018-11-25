(ns clj-insim.protocols
  (:require [clj-insim.enums :as enums]
            [clj-insim.util :refer [strip-null-chars]]))

(defn- bytes->int [c] (-> c first int))
(defn- bytes->string [c]
  (->>
   (map char (strip-null-chars c))
   (apply str)))
(defn- bytes->isp-type [c]
  (-> c first enums/isp-key))
(defn- bytes->tiny-subt [c]
  (-> c first enums/tiny-key))

(defn parse-protocol-map [{:keys [key type length]}]
  (case type
    :string {:bytes length :cast bytes->string :key key}
    :type {:bytes 1 :cast bytes->isp-type :key key}
    :tiny-subt {:bytes 1 :cast bytes->tiny-subt :key key}
    :word {:bytes 2 :cast #(map int %) :key key}
    :bytes {:bytes length :cast #(map int %) :key key}
    :int {:bytes 4 :cast #(map int %) :key key}
    {:bytes 1 :cast bytes->int :key key}))

(defn make-protocol [c]
  (into [] (map parse-protocol-map c)))

(def is-ver-protocol
  (make-protocol
   [{:key :type :type :type}
    {:key :reqi}
    {:key :zero}
    {:key :version :type :string :length 8}
    {:key :product :type :string :length 6}
    {:key :insim-version}
    {:key :spare}]))

(def is-tiny-protocol
  (make-protocol
   [{:key :type :type :type}
    {:key :reqi}
    {:key :subt :type :tiny-subt}]))

(def is-npl-protocol
  (make-protocol
   [{:key :type :type :type}
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
    {:key :spare-3}]))
