(ns clj-insim.types)

;; The second byte of any packet is one of these
(def ^:private isp-enum
  [:none :isi :ver :tiny :small
   :sta :sch :sfp :scc :cpp 
   :ism :mso :iii :mst :mtc
   :mod :vtn :rst :ncn :cnl
   :cpr :npl :plp :pll :lap
   :spx :pit :psf :pla :cch
   :pen :toc :flg :pfl :fin
   :res :reo :nlp :mci :msx :msl :crs :bfn :axi :axo :btn :btc :btt :rip :ssh :con :obh :hlv :plc :axm :acr :hcp :nci :jrr :uco :oco :ttc :slc :clc])

(defmulti isp type)
(defmethod isp clojure.lang.Keyword [k]
  (.indexOf isp-enum k))
(defmethod isp java.lang.Long [i]
  (nth isp-enum i))
(defmethod isp java.lang.Byte [b]
  (nth isp-enum (int b)))

(comment
  (isp 3)
  (isp :tiny)
)

;; The fourth byte of a IS_TINY packet is one of these
(def ^:private tiny-enum
  [:none :ver :close :ping :reply
   :vtc :scp :sst :gth :mpe
   :ism :ren :clr :ncn :npl
   :res :nlp :mci :reo :rst
   :axi :axc :rip :nci :alc
   :axm :slc])

(defmulti tiny type)
(defmethod tiny clojure.lang.Keyword [k]
  (.indexOf tiny-enum k))
(defmethod tiny java.lang.Long [i]
  (nth tiny-enum i))
(defmethod tiny java.lang.Byte [b]
  (nth tiny-enum (int b)))

(comment
  (tiny 3)
  (tiny :ping)
)
