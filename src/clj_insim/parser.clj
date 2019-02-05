(ns clj-insim.parser
  (:require [clj-insim.parser.util :as util]
            [clj-insim.parser.flags :as flags]
            [clj-insim.enums :as enums]
            [clj-insim.parser.post :as post]))

;;;;; PARSER PROTOCOLS ;;;;;
(defmulti protocol :type)

(defmethod protocol :cnl [_]
  [{:key :reason :parser #(-> % first enums/leave-reason-key)}
   :total :spare-2 :spare-3])

(defmethod protocol :fin [_]
  [(util/protocol-node :race-time :unsigned)
   (util/protocol-node :best-lap :unsigned)
   :spare-a :num-stops
   {:key :confirmation-flags :length 1 :parser #(-> % first flags/->confirmation)}
   :spare-b
   (util/protocol-node :laps-done :word)
   {:key :flags :length 2 :parser #(-> % util/->word flags/->player)}])

(defmethod protocol :flg [_]
  [:off-on {:key :flag :length 1 :parser #(-> % first enums/flg-flag-key)}
   :car-behind :spare-3])

(defmethod protocol :ism [_]
  [:host :spare-1 :spare-2 :spare-3
   {:key :host-name :length 32 :parser util/->string}])

(defmethod protocol :lap [_]
  [(util/protocol-node :lap-time :unsigned)
   (util/protocol-node :total-time :unsigned)
   (util/protocol-node :laps-done :word)
   (util/protocol-node :flags :word)
   :spare-0
   {:key :penalty :length 1 :parser #(-> % first enums/penalty-key)}
   :penlalty
   :num-stops :spare-3
   ])

(defmethod protocol :mso [header]
  [:uniq-connection-id :player-id
   {:key :user-type :length 1 :parser #(-> % first enums/mso-user-key)}
   :text-start
   {:key :message :length (- (:size header) 8) :parser util/->string}])

(defmethod protocol :nci [_]
  [{:key :language :parser #(-> % first enums/language-key)}
   :spare-1 :spare-2 :spare-3
   (util/protocol-node :user-id :unsigned)
   (util/protocol-node :ip-address :unsigned)])

(defmethod protocol :ncn [_]
  [{:key :user-name :length 24 :parser util/->string}
   {:key :player-name :length 24 :parser util/->string}
   {:key :admin :length 1 :parser #(-> % first pos?)}
   :total :flags :spare])

(defmethod protocol :npl [_]
  [:uniq-connection-id
   :player-type
;   {:key :player-type :length 1 :parser #(-> % first enums/npl-player-type-key)}
   {:key :player-flags :length 2 :parser #(-> % util/->word flags/->player)}
   {:key :player-name :length 24 :parser util/->string}
   {:key :plate :length 8 :parser util/->string}
   {:key :car-name :length 4 :parser util/->string}
   {:key :skin-name :length 16 :parser util/->string}
   {:key :tyres :length 4 :parser #(map enums/tyre-compounds-key %)}
   :handicap-mass :handicap-restriction :driver-model :passenger
   (util/protocol-node :spare :int)
   {:key :setup-flags :length 1 :parser #(-> % first flags/->setup)}
   :number-player :spare-2 :spare-3
   ])

(defmethod protocol :res [_]
  [{:key :user-name :length 24 :parser util/->string}
   {:key :player-name :length 24 :parser util/->string}
   {:key :plate :length 8 :parser util/->string}
   {:key :skin-prefix :length 4 :parser util/->string}
   (util/protocol-node :race-time :unsigned)
   (util/protocol-node :best-lap :unsigned)
   :spare-a :num-stops
   {:key :confirmation-flags :length 1 :parser #(-> % first flags/->confirmation)}
   :spare-b
   (util/protocol-node :laps-done :word)
   {:key :flags :length 2 :parser #(-> % util/->word flags/->player)}
   :result-num :num-results
   (util/protocol-node :penalty-time :word)])

(defmethod protocol :reo [_]
  [{:key :player-ids :length 40 :parser identity}])

(defmethod protocol :rst [_]
  [:race-laps :qualify-minutes :num-players :timing ;; TODO timing bits
   {:key :track :length 6 :parser util/->string}
   :weather :wind
   {:key :flags :length 2 :parser #(-> % util/->word flags/->race)}
   (util/protocol-node :num-nodes :word)
   (util/protocol-node :finish :word)
   (util/protocol-node :split1 :word)
   (util/protocol-node :split2 :word)
   (util/protocol-node :split3 :word)])

(defmethod protocol :sta [_]
  [{:key :replay-speed :length 4 :parser util/->string}
   {:key :flags :length 2 :parser #(-> % util/->word flags/->state)}
   :ingame-cam :viewed-player-id :num-players-in-race :num-connections :num-finished
   :race-in-progress :qualify-minutes :race-laps :spare-2 :spare-3
   {:key :track :length 6 :parser util/->string}
   :weather :wind])

(defmethod protocol :slc [_]
  [{:key :car-name :length 4 :parser util/->string}])

(defmethod protocol :ver [_]
  [{:key :version :length 8 :parser util/->string}
   {:key :product :length 6 :parser util/->string}
   :insim-version :spare])

(defmethod protocol :default [_]
  nil)

(defn- header
  "Parse the first 4 (header) bytes"
  [[size type reqi data]]
  {:size size :type (enums/isp-key type) :reqi reqi :data data})

(defn- assoc-protocol [result protocol colls]
  (let [parser (or (-> protocol first :parser) first)]
    (if-not (seq colls)
      result
      (recur
       (assoc result
         (or (-> protocol first :key) (first protocol))
         (parser (first colls)))
       (rest protocol)
       (rest colls)))))

;;;;; PUBLIC FUNCTIONS ;;;;;

(defn parse [packet]
  (when-let [header (header (take 4 packet))]
    (if-let [prtcl (protocol header)]
      (let [colls (reduce util/split-last [(drop 4 packet)] prtcl)
            body (assoc-protocol {} prtcl colls)]
        (post/parse (merge header body)))
      (post/parse header))))

;; (post/parse (parse [20 2 1 0 48 46 54 84 0 0 0 0 83 51 0 0 0 0 8 0]))

;;
