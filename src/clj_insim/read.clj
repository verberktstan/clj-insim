(ns clj-insim.read
  (:require [clj-insim.codecs :as codecs]
            [marshal.core :as m]))

(def ^:private TYPES
  [:none :isi :ver :tiny :small :sta :sch :sfp :scc :cpp :ism :mso :iii :mst :mtc :mod :vtn :rst :ncn
   :cnl :cpr :npl :plp :pll :lap :spx :pit :psf :pla :cch :pen :toc :flg :pfl :fin :res :reo :nlp
   :mci :msx :msl :crs :bfn :axi :axo :btn :btc :btt :rip :ssh :con :obh :hlv :plc :axm :acr :hcp
   :nci :jrr :uco :oco :ttc :slc :csc :cim])

(def ^:private DATA
  {:tiny
   [:none :ver :close :ping :reply
    :vtc  :scp :sst   :gth  :mpe
    :ism  :ren :clr   :ncn  :npl
    :res  :nlp :mci   :reo  :rst
    :axi  :axc :rip   :nci  :alc
    :axm  :slc]})

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

(defn- parse-header
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

(defn unparse-header [{:header/keys [type] :as header}]
  (let [data-enum (get DATA type)]
    (cond-> (update header :header/type #(.indexOf TYPES %))
      data-enum (update :header/data #(.indexOf data-enum %)))))

(defn- read-header
  "Reads 4 bytes from input-stream and returns these, marhalled into a clojure map.
   Returns false when no bytes are available to read."
  [input-stream]
  (and (pos? (.available input-stream))
       (m/read input-stream codecs/header)))

(def header (comp parse-header read-header))

(defn- get-body-codec
  "Returns the marshal codec for a given header (type).
   When the codec for this type can't be found, it returns a default unknown codec."
  [{:header/keys [size type]}]
  (codecs/body type (m/struct :body/unknown (m/ascii-string (- size 4)))))

;; TODO: Implement this
(def parse-body identity)

;; TODO: Implement this
(def unparse-body identity)

(defn- read-body [input-stream {:header/keys [size] :as header}]
  (when (> size 4)
    (m/read input-stream (get-body-codec header))))

(def body (comp parse-body read-body))
