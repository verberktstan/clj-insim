(ns examples.buttons
  (:require [clj-insim.client :as client]
            [clj-insim.packets :as packets]
            [examples.utils :as u]))

(def ^:private CLICK_ID 13)

(defmulti ^:private dispatch (fn [_ {:header/keys [type]}] type))
(defmethod dispatch :default [_ _] nil)

;; When the VER(sion) packet is received, send a button to LFS.
(defmethod dispatch :ver [client packet]
  (client/>!! client (packets/btn {:text "TEST button"
                                   :button-style #{:left :click}
                                   :click-id CLICK_ID})))

;; When a B(u)T(ton) C(lick) packet is received, check the click-id and notify if it is a match.
(defmethod dispatch :btc [client {:body/keys [click-id] :as packet}]
  (println packet)
  (when (= click-id CLICK_ID)
    (println "BTC packet received; clicked on the test button")))

(defn- buttons []
  (let [client (client/start)]
    (client/go client dispatch)
    #(client/stop client)))

(defn -main [& args]
  (u/main buttons))

(comment
  ;; To start the client
  (def buttons-client (buttons))

  ;; To stop the client
  (buttons-client)
)
