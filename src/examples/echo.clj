(ns examples.echo
  (:require [clj-insim.client :as client]
            [clj-insim.packets :as packets]
            [clojure.string :as str]))

;; An annotated example for using clj-insim

(defn- dispatch [client {:header/keys [type] :as packet}]
  (let [message (str "clj-insim: got a IS_" (-> type name str/upper-case) " packet from LFS.")]
    (when-not (= :mso type)
      ;; 4. Put a response (IS_MST packet) onto the channel
      (client/>! client (packets/mst {:message message})))))

(defn echo
  "Starts a simple echo process that prints all incoming packets from LFS to the
   console, and to LFS via a IS_MST packet. Well not ALL incoming packets, we
   ignore IS_MSO packets because this causes a feedback loop :-)."
  []
  (let [client (client/start) ;; 1. Start the client
        stop #(client/stop client)] ;; 2. Define a function that terminates client.
  (client/go client dispatch) ;; 3. Start a go block with a while-loop to check for packets
  stop)) ;; 5. Expose the stop function so we can use it elsewhere.

(comment

  ;; To start the echo process
  (def echo-client (echo))

  ;; To stop the client and echo process, simply call the stored function
  (echo-client)
)
