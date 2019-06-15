(ns clj-insim.enums)

(defn index->key [m]
  (fn [i]
    (->>
     m
     (filter #(= (val %) i))
     first
     key)))

(def cch-camera
  {:follow 0 :heli 1 :cam 2 :driver 3 :custom 4 :num 5})
(def cch-camera-key (index->key cch-camera))

(def csc-action
  {:stop 0 :start 1})
(def csc-action-key (index->key csc-action))

;; The sixth byte of a IS_FLG packet is one of these
(def flg-flag
  {:none 0 :blue-given 1 :yellow-caused 2})
(def flg-flag-key (index->key flg-flag))

;; The second byte of any packet is one of these
(def isp
  {:none 0 :isi 1 :ver 2 :tiny 3 :small 4
   :sta 5 :sch 6 :sfp 7 :scc 8 :cpp 9 
   :ism 10 :mso 11 :iii 12 :mst 13 :mtc 14
   :mod 15 :vtn 16 :rst 17 :ncn 18 :cnl 19
   :cpr 20 :npl 21 :plp 22 :pll 23 :lap 24
   :spx 25 :pit 26 :psf 27 :pla 28 :cch 29
   :pen 30 :toc 31 :flg 32 :pfl 33 :fin 34
   :res 35 :reo 36 :nlp 37 :mci 38 :msx 39
   :msl 40 :crs 41 :bfn 42 :axi 43 :axo 44
   :btn 45 :btc 46 :btt 47 :rip 48 :ssh 49
   :con 50 :obh 51 :hlv 52 :plc 53 :axm 54
   :acr 55 :hcp 56 :nci 57 :jrr 58 :uco 59
   :oco 60 :ttc 61 :slc 62 :csc 63 :cim 64})

(def isp-key (index->key isp))

(def language
  {:english 0 :deutsch 1 :portuguese 2 :french 3
   :suomi 4 :norsk 5 :nederlands 6 :catalan 7
   :turkish 8 :castellano 9 :italiano 10 :dansk 11
   :czech 12 :russian 13 :estionian 14 :serbian 15
   :greek 16 :polski 17 :croatian 18 :hungarian 19
   :brazilian 20 :swedish 21 :slovak 22 :galego 23
   :slovenski 24 :belarussian 25 :latvian 26 :lithuanian 27
   :traditional-chinese 28 :simplified-chinese 29 :japanese 30 :korean 31
   :bulgarian 32 :latino 33 :ukrainian 34 :indonesian 35
   :romanian 36 :num-lang 37})

(def language-key (index->key language))

;; The seventh byte of an IS_MSO packets is one of these
(def mso-user
  {:system 0 :user 1 :prefix 2 :o 3 :num 4})

(def mso-user-key (index->key mso-user))

;; The sixth byte of an IS_NPL packet is one of these
(def npl-player-type
  {:female 0
   :ai 2
   :remote-female 4
   :remote-ai 6})

(def npl-player-type-key (index->key npl-player-type))

(def tyre-compounds
  {:r1 0 :r2 1 :r3 2 :r4 3
   :road-super 4 :road-normal 5
   :hybrid 6 :knobbly 7
   :num 8})
(def tyre-compounds-key (index->key tyre-compounds))

;; The fifth and sixth byte of an IS_PEN packet are one of these
(def penalty
  {:none 0 :drive-through 1 :drive-through-valid 2 :stop-go 3 :stop-go-valid 4 :penalty-30 5 :penalty-45 6 :num 7})
(def penalty-key (index->key penalty))

;; The seventh byte of an IS_PEN packet is one of these
(def pen-reason
  {:unknown 0 :admin 1 :wrong-way 2 :false-start 3 :speeding 4 :stop-short 5 :stop-late 6 :num 7})
(def pen-reason-key (index->key pen-reason))

;; the fourth byte of an IS_SMALL packet is one of these
(def small
  {:none 0 :ssp 1 :ssg 2 :vta 3
   :tms 4 :stp 5 :rtp 6 :nli 7
   :alc 8 :lcs 9 :num 10})

(def small-key (index->key small))

;; The fourth byte of an IS_TINY packet is one of these
(def tiny
  {:none 0 :ver 1 :close 2 :ping 3 :reply 4
   :vtc 5 :scp 6 :sst 7 :gth 8 :mpe 9 
   :ism 10 :ren 11 :clr 12 :ncn 13 :npl 14
   :res 15 :nlp 16 :mci 17 :reo 18 :rst 19
   :axi 20 :axc 21 :rip 22 :nci 23 :alc 24
   :axm 25 :slc 26})

(def tiny-key (index->key tiny))

(def leave-reason
  {:disconnect 0 :time-out 1 :lost-connection 2 :kicked 3
   :banned 4 :security 5 :cpw 6 :oos 7
   :joos 8 :hack 9 :leave-reason-num 10})

(def leave-reason-key (index->key leave-reason))

(def jrr-action
  {:reject 0 :spawn 1 :reset 4 :reset-no-repair 5})

(def sta-race-in-progress {:none 0 :race 1 :qualification 2})

(def sta-race-in-progress-key (index->key sta-race-in-progress))

(def vtn-action
  {:none 0 :end 1 :restart 2 :qualify 3 :num 4})

(def vtn-action-key (index->key vtn-action))

(comment
  (:none isp) ; Return the index of :none in the isp enum
  (isp :none) ; Works too!

  (isp-key 21) ; Return the key of index 21 in the isp enum
  ((index->key isp) 21) ; Works too!
)
