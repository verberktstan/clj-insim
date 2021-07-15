(ns examples.utils
  (:require [clojure.string :as str]))

(defn main [client-fn]
  (let [insim-client (client-fn)]
    (println "Please type 'exit' to stop.")
    (loop [input ""]
      (if (#{"exit"} (str/lower-case input))
        (insim-client)
        (recur (read-line))))))
