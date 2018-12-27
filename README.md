# clj-insim

A Clojure library designed to interface with [Live For Speed](https://www.lfs.net/) [InSim](https://en.lfsmanual.net/wiki/InSim).

You can find it on Clojars: https://clojars.org/clj-insim/

## Usage

Add clj-insim to your project.clj:
```
[clj-insim "0.1.4-SNAPSHOT"]
```

Require clj-insim.socket/client fn, enums ns
```
(ns my-project.core
  (:require [clj-insim.socket :refer [client]]
            [clj-insim.packets :as [packets]]
            [clj-insim.enums :as enums]))
```

Define a handler to simply print incoming packets
```
(defn handler [packets]
  (do
    (doseq [packet packets]
      (println "\nclj-insim received a packet from lfs:")
      (prn packet)))
  (packets/is-tiny :none))
```

A handler receives a sequence of packets and should return a valid InSim packet (or a collection of packets). In this case we return the basic IS_TINY packet (keep alive), constructed with the is-tiny function in the packets namespace. We could also omit the fn argument in this case, because it defaults to `:none`.

Be sure to run LFS on your localhost and run `/insim 29999` to setup the TCP server from within LFS.

To start the TCP client, run the client function with the handler as argument. This returns an atom with it value set to true, you should store this in order to be able to disconnect lateron.
```
(def lfs-client (client handler))
```

You should now see notifications for each packet that LFS sends to clj-insim. Also, the raw bytes are printed, which is not particularly useful, yet.

To disconnect, reset the atom to false.
```
(reset! lfs-client false)
```

## License

Copyright © 2018 Stan Verberkt

Distributed under the Unlicense License.
This is free and unencumbered software released into the public domain.
For more information, please refer to <http://unlicense.org>
