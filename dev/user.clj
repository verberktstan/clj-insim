(ns user
  (:require [reloaded.repl :refer [system reset stop]]
            [clj-insim.client]))

(reloaded.repl/set-init! #'clj-insim.client/create-client)

(comment
  (reset)
  (stop)
)
