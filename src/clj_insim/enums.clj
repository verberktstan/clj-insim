(ns clj-insim.enums)

(defn- enum [enum k]
  (->> enum
       (keep-indexed #(when (= %2 k) %1))
       (first)))

(defn- make-enum-fn [v]
  (fn [x]
    (cond
      (keyword? x) (enum v x)
      (number? x) (nth v x)
      :else nil)))

(def view (make-enum-fn [:follow :heli :cam :driver :custom :num]))

(def csc (make-enum-fn [:stop :start]))

;; The second byte of any packet is one of these
(def isp
  (make-enum-fn
   [:none :isi :ver :tiny :small :sta :sch :sfp :scc :cpp 
    :ism :mso :iii :mst :mtc :mod :vtn :rst :ncn :cnl
    :cpr :npl :plp :pll :lap :spx :pit :psf :pla :cch
    :pen :toc :flg :pfl :fin :res :reo :nlp :mci :msx
    :msl :crs :bfn :axi :axo :btn :btc :btt :rip :ssh
    :con :obh :hlv :plc :axm :acr :hcp :nci :jrr :uco
    :oco :ttc :slc :csc :cim]))

#_(isp 43)
#_(isp :axi)

(def language
  (make-enum-fn
   [:english :deutsch :portuguese :french :suomi :norsk :nederlands :catalan
    :turkish :castellano :italiano :dansk :czech :russian :estionian :serbian
    :greek :polski :croatian :hungarian :brazilian :swedish :slovak :galego
    :slovenski :belarussian :latvian :lithuanian :traditional-chinese :simplified-chinese :japanese :korean
    :bulgarian :latino :ukrainian :indonesian :romanian :num-lang]))

;; The seventh byte of an IS_MSO packets is one of these
(def mso (make-enum-fn [:system :user :prefix :o :num]))

;; The sixth byte of an IS_NPL packet is one of these
(def npl (make-enum-fn [:female :female :ai :ai :remote-female :remote-female :remote-ai :remote-ai]))

(def tyre (make-enum-fn [:r1 :r2 :r3 :r4 :road-super :road-normal :hybrid :knobbly :num]))

;; The fifth and sixth byte of an IS_PEN packet are one of these
(def penalty (make-enum-fn [:none :drive-through :drive-through-valid :stop-go :stop-go-valid :penalty-30 :penalty-45 :num]))

;; The seventh byte of an IS_PEN packet is one of these
(def penr (make-enum-fn [:unknown :admin :wrong-way :false-start :speeding :stop-short :stop-late :num]))

;; the fourth byte of an IS_SMALL packet is one of these
(def small (make-enum-fn [:none :ssp :ssg :vta :tms :stp :rtp :nli :alc :lcs :num]))

;; The fourth byte of an IS_TINY packet is one of these
(def tiny
  (make-enum-fn
   [:none :ver :close :ping :reply :vtc :scp :sst :gth :mpe 
    :ism :ren :clr :ncn :npl :res :nlp :mci :reo :rst
    :axi :axc :rip :nci :alc :axm :slc]))

(def leavr
  (make-enum-fn
   [:disconnect :time-out :lost-connection :kicked :banned :security :cpw :oos :joos :hack :leave-reason-num]))

(def jrr
  (make-enum-fn
   [:reject :spawn :two :three :reset :reset-no-repair :six :seven]))

(def race-in-prog (make-enum-fn [:none :race :qualification]))

(def vote (make-enum-fn [:none :end :restart :qualify :num]))
