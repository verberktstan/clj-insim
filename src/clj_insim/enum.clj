(ns clj-insim.enum
  (:require [clj-insim.utils :as u]))

(def ACTION [:none :end :restart :qualify])

(def COMPOUNDS [:r1 :r2 :r3 :r4 :road-super :road-normal :hybrid :knobbly])

(def HEADER_TYPE
  [:none :isi :ver :tiny :small :sta :sch :sfp :scc :cpp :ism :mso :iii :mst :mtc
   :mod :vtn :rst :ncn :cnl :cpr :npl :plp :pll :lap :spx :pit :psf :pla :cch
   :pen :toc :flg :pfl :fin :res :reo :nlp :mci :msx :msl :crs :bfn :axi :axo
   :btn :btc :btt :rip :ssh :con :obh :hlv :plc :axm :acr :hcp :nci :jrr :uco :oco
   :ttc :slc :csc :cim])

(def HOST [:guest :host])

(def LEAVE_REASONS
  [:disconnect :timeout :lost-connection :kicked :banned :security :cpw :oos
   :joos :hack])

(def PENALTY [:none :drive-through :drive-through-valid :stop-go :stop-go-valid :penalty-30 :penalty-45])

(def PLAYER_TYPE [:female :ai :remote])

(def RACE_IN_PROGRESS [:no-race :race :qualifying])

(def SFP [:shift-u-no-opt :show-2d :mspeedup :sound-mute])

(def SMALL_HEADER_DATA [:none :ssp :ssg :vta :tms :stp :rtp :nli :alc :lcs])

(def STOP [:carry-on :stop])

(def TINY_HEADER_DATA
  [:none :ver :close :ping :reply :vtc :scp :sst :gth :mpe :ism :ren :clr :ncn
   :npl :res :nlp :mci :reo :rst :axi :axc :rip :nci :alc :axm :slc])

(def TTC_HEADER_DATA [:none :sel :sel-start :sel-stop])

(def USER_TYPE [:system :user :prefix :o])

(def VIEW_IDENTIFIERS [:follow :heli :cam :driver :custom])

(def WIND [:off :weak :strong])

(defn encode
  "Returns a function that returns the integer value of item in enum.
   `((encode [:a :b]) :b) => 1`"
  [enum]
  {:pre [(sequential? enum)]}
  (u/index-of enum))

(defn decode
  "Returns a function that returns the item found at an index in enum.
   `((decode [:a :b]) 1) => :b`"
  [enum]
  {:pre [(sequential? enum)]}
  (fn [idx]
    (when (contains? enum idx)
      (nth enum idx))))
