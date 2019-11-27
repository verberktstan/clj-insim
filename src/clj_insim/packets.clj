(ns clj-insim.packets)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Packets

(defn insim-init
  ([]
   (insim-init {}))
  ([{:keys [admin iname]}]
   {:header
    {:size 44 :type :isi :request-info 1 :subtype 0}
    :body
    {:udp-port 0 :flags 0 :insim-version 8 :prefix (int \!) :interval 0
     :admin (apply str (take 16 (or admin "pwd")))
     :iname (apply str (concat (take 15 (or iname "clj-insim")) [(char 0)]))}}))

(defn tiny
  ([]
   (tiny {}))
  ([{:keys [request-info subtype]}]
   {:header
    {:size 4 :type :tiny :request-info (or request-info 0) :subtype (or subtype :none)}}))

(defn- c-stringify [s]
  (let [new-s (str s (char 0))]
    (if (zero? (-> new-s count (mod 4)))
      new-s
      (recur new-s))))

(defn mst [message]
  {:header
   {:size 68 :type :mst :request-info 0 :subtype 0}
   :body
   {:message message}})
