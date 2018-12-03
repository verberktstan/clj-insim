(ns clj-insim.enums)

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
   :btn 45 :btc 46 :btt 47 :rip 48  :ssh 49
   :con 50 :obh 51 :hlv 52 :plc 53 :axm 54
   :acr 55 :hcp 56 :nci 57 :jrr 58 :uco 59
   :oco 60 :ttc 61 :slc 62 :csc 63})

;; The fourth byte of an IS_TINY packet is one of these
(def tiny
  {:none 0 :ver 1 :close 2 :ping 3 :reply 4
   :vtc 5 :scp 6 :sst 7 :gth 8 :mpe 9 
   :ism 10 :ren 11 :clr 12 :ncn 13 :npl 14
   :res 15 :nlp 16 :mci 17 :reo 18 :rst 19
   :axi 20 :axc 21 :rip 22 :nci 23 :alc 24
   :axm 25 :slc 26})

(def jrr-action
  {:reject 0 :spawn 1 :reset 4 :reset-no-repair 5})

(def vtn-action
  {:none 0 :end 1 :restart 2 :qualify 3 :num 4})

(defn index->key [m]
  (fn [i]
    (->>
     m
     (filter #(= (val %) i))
     first
     key)))

(def isp-key (index->key isp))
(def tiny-key (index->key tiny))
(def vtn-action-key (index->key vtn-action))

(comment
  (:none isp) ; Return the index of :none in the isp enum
  (isp :none) ; Works too!

  (isp-key 21) ; Return the key of index 21 in the isp enum
  ((index->key isp) 21) ; Works too!
)
