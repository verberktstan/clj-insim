(ns clj-insim.models.packet
  (:refer-clojure :exclude [read])
  (:require [clojure.spec.alpha :as s]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Specs

(s/def ::header (s/keys :req-un [::size ::type ::request-info ::data]))
(s/def ::body associative?)

(s/def ::model
  (s/keys :req [::header]
          :opt [::body]))

(defn make [header body]
  (merge {::header header} (when body {::body body})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Checks

(defn tiny-none? [{::keys [header]}]
  (and (#{:tiny} (:type header))
       (zero?    (:request-info header))
       (#{:none} (:data header))))

(defn reply? [{::keys [header]}]
  (not= 0 (:request-info header)))

(defn data [{::keys [header]}]
  (:data header))

(defn ncn? [{::keys [header]}]
  (boolean (#{:ncn} (:type header))))

(defn npl? [{::keys [header]}]
  (boolean (#{:npl} (:type header))))

(defn cnl? [{::keys [header]}]
  (boolean (#{:cnl} (:type header))))

(defn pll? [{::keys [header]}]
  (boolean (#{:pll} (:type header))))

(def ^:private dissoc-spares
  #(dissoc % :spare0 :spare1 :spare2 :spare3))

(defn ncn->connection [{::keys [body] :as packet}]
  (when (ncn? packet)
    (let [connection-id (data packet)]
      (-> body
          (dissoc-spares)
          (assoc :connection-id connection-id)))))

(defn cnl->connection [{::keys [body] :as packet}]
  (when (cnl? packet)
    (let [connection-id (data packet)]
      (-> body
          (dissoc-spares)
          (assoc :connection-id connection-id)))))

(defn npl->player [{::keys [body] :as packet}]
  (when (npl? packet)
    (let [player-id (data packet)]
      (-> body
          (dissoc-spares)
          (assoc :player-id player-id)))))

(defn pll->player [{::keys [header] :as packet}]
  (when (pll? packet)
    {:player-id (data packet)}))
