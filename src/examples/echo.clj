(ns examples.echo
  (:require [clj-insim.client :as client]
            [clj-insim.packets :as packets]
            [clojure.core.async :as a]
            [clojure.string :as str]))

;; An annotated example for using clj-insim

#_(defn echo
  "Starts a simple echo process that prints all incoming packets from LFS to the
   console, and to LFS via a IS_MST packet. Well not ALL incoming packets, we
   ignore IS_MSO packets because this causes a feedback loop :-)."
  []  
  (let [;; 1. Start the clj-insim client
        client (client/start-client)
        running? (atom true)
        ;; 2. Define a function that terminates this process and the clj-insim client.
        stop! #(do (reset! running? false) (client/stop! client))]
    (a/go
      ;; 3. Start a go block with a while-loop to check for packets
      (while @running?
        (let [;; 4. Retreive a single packet from the channel
              {:header/keys [type] :as packet} (a/<! (::client/from-lfs-chan client))
              message (str "clj-insim: got a IS_" (-> type name str/upper-case) " packet from LFS.")]
          ;; Print stuff to the console
          (newline)
          (println message)
          (println packet)
          (when-not (= :mso type)
            ;; 5. Put a response (IS_MST packet) onto the channel
            (a/>! (::client/to-lfs-chan client) (packets/mst {:message message}))))))
    ;; 6. Expose the stop! function so we can use it elsewhere.
    {:stop! stop!}))

(comment
  ;; To start the echo process
  (def lfs-client (echo))

  ;; To stop the client and echo process
  (let [{:keys [stop!]} lfs-client]
    (stop!))
)


