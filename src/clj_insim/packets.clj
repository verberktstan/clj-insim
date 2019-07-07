(ns clj-insim.packets)

(defn isi
  ([]
   (isi {}))
  ([{:keys [admin flags iname insim-version interval prefix]}]
   {:size 44 :type (:isi enums/ISP) :reqi 1 :zero 0
    :udp-port 0 :flags (or flags 0) :insim-version (or insim-version 8) :prefix (int (char (or prefix \!)))
    :interval (or interval 0) :admin (or admin "") :iname (or iname "clj-insim")}))

(defn tiny [sub-type]
  {:size 4 :type :tiny :reqi 1 :sub-type sub-type})

(defn msl [message]
  {:size 132 :type :msl :reqi 1 :sound 0 :message message})
