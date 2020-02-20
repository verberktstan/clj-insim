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
