(ns clj-insim.packets
  (:require [clj-insim.enums :as enums]))

(defn isi
  ([]
   (isi {}))
  ([{:keys [admin flags iname insim-version interval prefix]}]
   {:header {:size 44 :type :isi :reqi 1 :data 0}
    :body {:udp-port 0 :flags (or flags 0) :insim-version (or insim-version 8) :prefix (int (char (or prefix \!)))
           :interval (or interval 0) :admin (or admin "") :iname (or iname "clj-insim")}}))

(defn tiny [sub-type]
  {:header
   {:size 4 :type :tiny :reqi 1 :sub-type sub-type}
   :body
   {}})

(defn msl [message]
  {:header
   {:size 132 :type :msl :reqi 1 :sound 0}
   :body
   {:message message}})
