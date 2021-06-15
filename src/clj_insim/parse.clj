(ns clj-insim.parse
  (:require [clj-insim.flags :as flags]
            [clj-insim.utils :as u]))

(def ^:private TYPES
  [:none :isi :ver :tiny :small :sta :sch :sfp :scc :cpp :ism :mso :iii :mst :mtc :mod :vtn :rst :ncn
   :cnl :cpr :npl :plp :pll :lap :spx :pit :psf :pla :cch :pen :toc :flg :pfl :fin :res :reo :nlp
   :mci :msx :msl :crs :bfn :axi :axo :btn :btc :btt :rip :ssh :con :obh :hlv :plc :axm :acr :hcp
   :nci :jrr :uco :oco :ttc :slc :csc :cim])

(def ^:private DATA
  {:small
   [:none :ssp :ssg :vta :tms :stp :rtp :nli :alc :lcs]

   :tiny
   [:none :ver :close :ping :reply
    :vtc  :scp :sst   :gth  :mpe
    :ism  :ren :clr   :ncn  :npl
    :res  :nlp :mci   :reo  :rst
    :axi  :axc :rip   :nci  :alc
    :axm  :slc]

   :ttc
   [:none :sel :sel-start :sel-stop]})

(def ^:private STA_FLAGS
  [:game :replay :paused :shift-u :dialog :shift-u-follow
   :shift-u-no-opt :show-2d :front-end :multi :mspeedup :windowed
   :sound-mute :view-override :visible :text-entry])

(def ^:private BODY_PARSERS
  {:sta
   #:body{:flags (partial flags/parse STA_FLAGS)}})

(defn- parse-header-data
  "Returns header with `:header/data` parsed.
   ```clojure
  (parse-data #:header{:type :tiny :data 2})
  => #:header{:type :tiny :data :close}
  ```"
  [{:header/keys [type] :as header}]
  (let [data-enum (get DATA type)]
    (cond-> header
      data-enum (update :header/data (partial nth data-enum)))))

(defn header
  "Returns header with `:header/type` and `:header/data` parsed. Returns `nil` when header or type are falsey.
   ```clojure
  (parse-data #:header{:type 3 :data 2})
  => #:header{:type :tiny :data :close}
  ```"
  [{:header/keys [type] :as header}]
  (when (and header type)
    (-> header
        (update :header/type (partial nth TYPES))
        (parse-header-data))))

;; TODO: Implement this
(defn body [{:header/keys [type] :as packet}]
  (let [parsers (get BODY_PARSERS type)]
    (println parsers)
    (println packet)
    (cond-> packet
      parsers (u/map-kv parsers))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Unparse

(def ^:private BODY_UNPARSERS
  {:isi
   #:body{:admin #(u/c-str % 16)
          :iname #(u/c-str % 16)
          :prefix int}

   :sta
   #:body{:flags (partial flags/unparse STA_FLAGS)}})

(defn- unparse-body [{:header/keys [type] :as packet}]
  (let [unparsers (get BODY_UNPARSERS type)]
    (cond-> packet
      unparsers (u/map-kv unparsers))))

(defn- unparse-header [{:header/keys [type] :as header}]
  (let [data-enum (get DATA type)]
    (cond-> (update header :header/type #(.indexOf TYPES %))
      data-enum (update :header/data #(.indexOf data-enum %)))))

(def unparse (comp unparse-header unparse-body))
