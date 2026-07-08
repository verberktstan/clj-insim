(ns clj-insim.enum
  (:require [clj-insim.utils :as u]))

(def ACTION [:none :end :restart :qualify])

(def BFN_HEADER_DATA [:delete-button :clear :user-clear :request])

(def COMPOUNDS [:r1 :r2 :r3 :r4 :road-super :road-normal :hybrid :knobbly])

(def HEADER_TYPE
  [:none :isi :ver :tiny :small :sta :sch :sfp :scc :cpp :ism :mso :iii :mst :mtc
   :mod :vtn :rst :ncn :cnl :cpr :npl :plp :pll :lap :spx :pit :psf :pla :cch
   :pen :toc :flg :pfl :fin :res :reo :nlp :mci :msx :msl :crs :bfn :axi :axo
   :btn :btc :btt :rip :ssh :con :obh :hlv :plc :axm :acr :hcp :nci :jrr :uco :oco
   :ttc :slc :csc :cim :mal :plh :ipb :aic :aii :set])

(def HOST [:guest :host])

(def LEAVE_REASONS
  [:disconnect :timeout :lost-connection :kicked :banned :security :cpw :oos
   :joos :hack])

(def MESSAGE_SOUNDS [:silent :message :system-message :invalid-key :error])

(def PENALTY
  [:none :drive-through :drive-through-valid :stop-go :stop-go-valid :penalty-30
   :penalty-45])

(def PENALTY_REASONS
  [:unknown :admin :wrong-way :false-start :speeding :stop-short :stop-late])

(def PIT_LANE_FACTS [:exit :enter :no-purpose :drive-through :stop-go])

(def PLAYER_TYPE [:female :remote :ai]) ;; NOTE: Different from docs!

(def PMO_ACTION [:loading-file :add-objects :delete-objects :clear-all :tiny-axm :ttc-sel :selection :position :get-z])

(def RACE_IN_PROGRESS [:no-race :race :qualifying])

(def SFP [:shift-u-no-opt :show-2d :mspeedup :sound-mute])

(def SMALL_HEADER_DATA [:none :ssp :ssg :vta :tms :stp :rtp :nli :alc :lcs :lcl :aii])

(def STOP [:carry-on :stop])

(def TINY_HEADER_DATA
  [:none :ver :close :ping :reply :vtc :scp :sst :gth :mpe :ism :ren :clr :ncn
   :npl :res :nlp :mci :reo :rst :axi :axc :rip :nci :alc :axm :slc :mal])

(def TTC_HEADER_DATA [:none :sel :sel-start :sel-stop])

(def USER_TYPE [:system :user :prefix :o])

(def VIEW_IDENTIFIERS [:follow :heli :cam :driver :custom])

(def WIND [:off :weak :strong])

;; AI Control input types (for IS_AIC)
(def AI_CONTROL_INPUTS
  [:steer :throttle :brake :shift-up :shift-down :ignition :extralight :headlights
   :siren :horn :flash :clutch :handbrake :indicators :gear :look
   :pitspeed :tcdisable :fogrear :fogfront])

;; AI Control special commands (values 240-255)
(def AI_CONTROL_SPECIAL
  {:send-ai-info 240
   :repeat-ai-info 241
   :set-help-flags 253
   :reset-inputs 254
   :stop-control 255})

;; AI Flags (for IS_AII)
(def AI_FLAGS [:ignition :reserved-2 :shift-up :shift-down])

;; Headlights values (for AI control)
(def HEADLIGHTS [:off :side :low :high])

;; Siren types (for AI control)
(def SIREN_TYPES [:off :fast :slow])

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
