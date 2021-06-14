(ns clj-insim.core
  (:require [clj-insim.codecs :as codecs]
            [clj-insim.read :as read]
            [clj-insim.utils :as u]
            [clojure.core.async :as a]
            [clojure.java.io :as io]
            [marshal.core :as m])
  (:import [java.net Socket]))

;; InSimInit packet
(def isi
  {:header/size 44 :header/type :isi :header/request-info 1 :header/data 0
   :body/udp-port 0 :body/is-flags 0 :body/insim-version 8 :body/prefix (int \!)
   :body/interval 0 :body/admin (u/c-str "" 16) :body/iname (u/c-str "clj-insim" 16)})

(defn client []
  (let [running? (atom true)
        to-lfs-chan (a/chan (a/sliding-buffer 2))
        from-lfs-chan (a/chan (a/sliding-buffer 2))
        socket (Socket. "127.0.0.1" 29999) ;; TODO: Pass in host/port as arguments
        output-stream (io/output-stream socket)
        input-stream (io/input-stream socket)
        close! (fn []
                 (.close input-stream)
                 (.close output-stream)
                 (.close socket))]
    (a/go
      (a/>! to-lfs-chan isi) ;; TODO: Pass in the ISI packet as argument
      (while @running?
        (let [{:header/keys [size type] :as data} (a/<! to-lfs-chan)]
          (m/write output-stream codecs/header (read/unparse-header data))
          (when (> size 4)
            (m/write output-stream (get codecs/body type) (read/unparse-body data)))
          (.flush output-stream))))
    (a/go
      (while @running?
        (when-let [header (read/header input-stream)]
          (a/>!
           from-lfs-chan
           (merge header (read/body input-stream header))))))
    {:running? running?
     :to-lfs-chan to-lfs-chan
     :from-lfs-chan from-lfs-chan
     :close! close!}))

(defn stop! [{:keys [close! running?]}]
  (reset! running? false)
  (close!))

(comment
  (def lfs-client (client))
  (a/go (println (a/<! (:from-lfs-chan lfs-client))))
  (stop! lfs-client)
)
