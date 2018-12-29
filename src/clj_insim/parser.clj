(ns clj-insim.parser
  (:require [clj-insim.parser.util :as util]
            [clj-insim.parser.flags :as flags]
            [clj-insim.enums :as enums]))

;;;;; PARSER PROTOCOLS ;;;;;
(defmulti protocol :type)

(defmethod protocol :rst [_]
  [:race-laps :qualify-minutes :num-players :timing
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
        (merge header body))
      header)))

;; (parse [20 2 1 0 48 46 54 84 0 0 0 0 83 51 0 0 0 0 8 0])
