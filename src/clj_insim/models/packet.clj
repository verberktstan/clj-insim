(ns clj-insim.models.packet
  (:refer-clojure :exclude [read type])
  (:require [clj-insim.utils :as u]
            [clojure.spec.alpha :as s]))

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

(defn tiny? [{::keys [header] :as p}]
  (when (#{:tiny} (:type header))
    p))

(defn none? [{::keys [header] :as p}]
  (when (u/equal-keys? {:request-info 0, :data :none} header)
    p))

(defn clear? [{::keys [header] :as p}]
  (when (u/equal-keys? {:data :clr} header)
    p))

(def tiny-none? (comp none? tiny?))

(def tiny-clear? (comp clear? tiny?))

(defn reply? [{::keys [header]}]
  (not= 0 (:request-info header)))

(defn data [{::keys [header]}]
  (:data header))

(def ^:private dissoc-spares
  #(dissoc % :spare0 :spare1 :spare2 :spare3))

(defn ncn->connection [{::keys [header body] :as packet}]
  (when (u/equal-keys? {:type :ncn} header)
    (let [connection-id (data packet)]
      (-> body
          (dissoc-spares)
          (assoc :connection-id connection-id)))))

(defn cnl->connection [{::keys [header body] :as packet}]
  (when (u/equal-keys? {:type :cnl} header)
    (let [connection-id (data packet)]
      (-> body
          (dissoc-spares)
          (assoc :connection-id connection-id)))))

(defn npl->player [{::keys [header body] :as packet}]
  (when (u/equal-keys? {:type :npl} header)
    (let [player-id (data packet)]
      (-> body
          (dissoc-spares)
          (assoc :player-id player-id)))))

(defn pll->player [{::keys [header] :as packet}]
  (when (u/equal-keys? {:type :pll} header)
    {:player-id (data packet)}))

(defn type [{::keys [header]}]
  (:type header))
