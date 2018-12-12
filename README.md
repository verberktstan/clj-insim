# clj-insim

A Clojure library designed to interface with Live For Speed InSim.

## Usage

```
;; Define a handler that prints the parsed packet and ALWAYS returns an IS_TINY packet.
(defn handler [packet]
  (let [{:keys [type] :as parsed-packet} (parse packet)]
    (do
      (println "\nReceived " (name type) " packet from LFS")
      (prn parsed-packet)
      (packets/is-tiny))))
      
;; Start a tcp client with a handler
(def simple-client (client handler))

;; To stop the client
(reset! simple-client false)
```

## License

Copyright © 2018 Stan Verberkt

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
