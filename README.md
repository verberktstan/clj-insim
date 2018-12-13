# clj-insim

A Clojure library designed to interface with [Live For Speed](https://www.lfs.net/) [InSim](https://en.lfsmanual.net/wiki/InSim).

## Usage

Add clj-insim to your project.clj:
```
[clj-insim "0.1.0-SNAPSHOT"]
```

Require packets, parser and socket namespaces.
```
(ns something.core
  (:require [clj-insim.packets :as packets]
            [clj-insim.parsers :refer [parse]]
            [clj-insim.socket :refer [client]]))
```

Define a handler for incoming InSim packets.
```
(defn handler [[type :as packet]]
  (let [{:keys [type] :as parsed} (parse packet)] ;; parse the packet
    (if type ;; if type is not nil
      (do
        (println (str "\nReceived " (name type) " packet from LFS"))
        (prn parsed)) ;; Print the parsed packet
      (do
        (println (str "\nCouldn't parse incoming packet from LFS"))
        (prn packet))) ;; Print the incoming byte-array
    (packets/is-tiny))) ;; ALWAYS return some packet, supply is-tiny to maintain connection..
```

Be sure to run LFS on your localhost and run `/insim 29999` to setup the TCP server from within LFS.

Start an LFS client with our handler.
```
(def lfs-client (client handler))
```

Stop the client by resetting the atom to false.
```
(reset! lfs-client false)
```

## License

Copyright © 2018 Stan Verberkt

Distributed under the Unlicense License.
This is free and unencumbered software released into the public domain.
For more information, please refer to <http://unlicense.org>
