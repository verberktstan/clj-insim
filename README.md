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
            [clj-insim.enums :as enums]
            [clj-insim.core :refer [parse]]))
```

Define a handler to simply print an incoming packet
```
(defn print-handler
  "Print a (parsed) packet and return the IS_TINY keepalive packet"
  [packet]
  (let [{:keys [type] :as parsed} (parse packet)]
    (println (str "\n== Received a " (name type) " packet from LFS =="))
    (prn parsed)
    (packets/is-tiny)))
```

A handler receives a sequence of packets and should return a valid InSim packet (or a collection of packets). In this case we return the basic IS_TINY packet (keep alive), constructed with the is-tiny function in the packets namespace.

Be sure to run LFS on your localhost and run `/insim 29999` to setup the TCP server from within LFS.

To start the TCP client, run the client function with the handler as argument. This returns an atom with it value set to true, you should store this in order to be able to disconnect lateron. As the handler function receives a collection of packets, you should actually map your handler function across the incoming argument.

```
(def lfs-client (client (partial map print-handler)))
```

You should now see notifications for each packet that LFS sends to clj-insim. Also, the raw bytes are printed, which is not particularly useful, yet.

To disconnect, reset the atom to false.
```
(reset! lfs-client false)
```

### Dispatching

Define a function to handle an incoming packet.

```
(defn check-version
  "Returns a welcome message if LFS version of host is 0.6T and insim-version is greater than 7. Else returns an IS_TINY packet that closes the connection."
  [{:keys [version insim-version]}]
  (if (and (= version "0.6T")
           (= insim-version 8))
    (packets/is-msl "Warm welcome from clj-insim!")
    (packets/is-tiny {:data-key :close})))
```

Define a map with dispatchers, mapping a function to an incoming packet. In this case, when the incoming packet is of :type :ver (IS_VER) then call the check-version function.

```
(def dispatchers
  {:ver check-version})

(defn handler
  "Parse packet, print notification and call dispatch fn OR return the IS_TINY keepalive packet."
  [packet]
  (let [{:keys [type] :as parsed} (parse packet)]
    (println (str "\n== Received a " (name type) " packet from LFS =="))
    (or (dispatch dispatchers parsed {:debug true}) (packets/is-tiny))))

(def lfs-client (client (partial map handler)))
```

This way you can define multiple dispatchers and do cool stuff!

## License

Copyright © 2018 Stan Verberkt

Distributed under the Unlicense License.
This is free and unencumbered software released into the public domain.
For more information, please refer to <http://unlicense.org>
