(ns clj-insim.types)

(def ISP-ENUM {:none 0
               :isi 1
               :ver 2
               :tiny 3
               :small 4
               :sta 5
               :mst 13})

(defn isp
  "Returns the enumeration for insim packet types. To get ISP_ISI (1) execute (isp :isi)."
  [k]
  (get ISP-ENUM k))

(comment
  (isp :isi)
  (isp :mst)
)
