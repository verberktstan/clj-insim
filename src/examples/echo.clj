(ns examples.echo
  (:require [clj-insim.client :as client]
            [clj-insim.packets :as packets]
            [clojure.core.async :as a]
            [clojure.string :as str]))

;; An annotated example for using clj-insim

(defn echo
  "Starts a simple echo process that prints all incoming packets from LFS to the
   console, and to LFS via a IS_MST packet. Well not ALL incoming packets, we
   ignore IS_MSO packets because this causes a feedback loop :-)."
  []
  (let [client (client/start)
        running? (atom true)
        ;; 1. Define a function that terminates this process and the clj-insim client.
        stop #(do (reset! running? false) (client/stop client))]
    ;; 2. Enable verbose logging in clj-insim
  (reset! client/VERBOSE true)
    (a/go
      ;; 3. Start a go block with a while-loop to check for packets
      (while @running?
        ;; 4. Retreive a single packet from the channel
        (let [{:header/keys [type] :as packet} (a/<! (:from-lfs client))
              message (str "clj-insim: got a IS_" (-> type name str/upper-case) " packet from LFS.")]
          (when-not (= :mso type)
            ;; 5. Put a response (IS_MST packet) onto the channel
            (a/>! (:to-lfs client) (packets/mst {:message message}))))))
    ;; 6. Expose the stop function so we can use it elsewhere.
    stop))

(comment

  ;; To start the echo process
  (def echo-client (echo))

  ;; To stop the client and echo process, simply call the stored function
  (echo-client)
)


