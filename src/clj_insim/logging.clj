(ns clj-insim.logging
  "Verbose packet logging and error tracking for the InSim client."
  (:require [clojure.string :as str]))

(def ERROR_LOG (atom nil))
(defonce ERRORS (atom true))
(defonce VERBOSE (atom false))

(defn print-verbose [packet]
  (when @VERBOSE
    (newline)
    (println (str "IS_" (-> (:header/type packet) name str/upper-case) " packet!"))
    (println (str packet))))

(defn log-throwable [t]
  (when @ERRORS
    (swap! ERROR_LOG conj (Throwable->map t))
    (println "clj-insim error:" (.getMessage t))))

(defn wrap-try-catch [f & args]
  (try (apply f args) (catch Throwable t (log-throwable t))))
