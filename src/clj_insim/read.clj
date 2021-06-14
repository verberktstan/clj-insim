(ns clj-insim.read
  (:require [clj-insim.codecs :as codecs]
            [clj-insim.parse :as parse]
            [clj-insim.utils :as u]
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

(def ^:private BODY_UNPARSERS
  {:isi
   #:body{:admin #(u/c-str % 16)
          :iname #(u/c-str % 16)
          :prefix int}})

(defn- read-header
  "Reads 4 bytes from input-stream and returns these, marhalled into a clojure map.
   Returns false when no bytes are available to read."
  [input-stream]
  (and (pos? (.available input-stream))
       (m/read input-stream codecs/header)))

(def header (comp parse/header read-header))

(defn- get-body-codec
  "Returns the marshal codec for a given header (type).
   When the codec for this type can't be found, it returns a default unknown codec."
  [{:header/keys [size type]}]
  (codecs/body type (m/struct :body/unknown (m/ascii-string (- size 4)))))

(defn- read-body [input-stream {:header/keys [size] :as header}]
  (when (> size 4)
    (m/read input-stream (get-body-codec header))))

(def body (comp parse/body read-body))
