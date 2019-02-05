(ns clj-insim.connection
  (:require [clj-insim.packets :as packets]))

(defonce connections (atom {}))
;; @connections
;; (count @connections)
;; (reset! connections nil)

(defn register! [{:keys [uniq-connection-id total] :as connection} {:keys [check-total-connections? notify-host?]}]
  (swap! connections assoc uniq-connection-id (dissoc connection :type :size :reqi :spare :total))
  [(when (and check-total-connections? (not= (count @connections) total)) ;; when not in sync
     (reset! connections nil) ;; Reset connection map
     (packets/is-tiny {:data-key :ncn})) ;; Request all connections
   (when notify-host? (packets/is-msl (str "clj-insim: connection " uniq-connection-id " registered!")))])

(defn unregister! [{:keys [uniq-connection-id] :as connection} {:keys [notify-host?]}]
  (swap! connections dissoc uniq-connection-id)
  (when notify-host? (packets/is-msl (str "clj-insim: connection " uniq-connection-id " UNregistered!"))))

