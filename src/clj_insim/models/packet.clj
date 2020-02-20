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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Checks

(defn tiny-none? [{::keys [header]}]
  (and (#{:tiny} (:type header))
       (zero?    (:request-info header))
       (#{:none} (:data header))))

(defn join-request? [{::keys [header body]}]
  (and (#{:npl} (:type header))
       (#{0} (:npl/num-players body))))
