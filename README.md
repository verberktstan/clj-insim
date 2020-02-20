# clj-insim

A Clojure library designed to interface with [Live For Speed](https://www.lfs.net/) via the [InSim protocol](https://en.lfsmanual.net/wiki/InSim).

You can find it on Clojars: https://clojars.org/clj-insim/

## Usage

Add clj-insim to your project.clj:
```
[clj-insim "0.1.9-SNAPSHOT"]
```

Require the clj-insim.core and clj-insim.packets ns:
```
(ns my-project.core
  (:require [clj-insim.core :as clj-insim]
            [clj-insim.packets :as packets]
            [clj-insim.models.packet :as packet]))
```

### Very basic default client

Make sure to start InSim from LFS by executing `/insim 29999`.
To set up a client with the default handler:
```
(def lfs-client (clj-insim/client))
```
By default, the client simply prints the incoming packets to the repl.

To send a message to LFS, call `enqueue!` and pass the client and a MTC (message to connection) packet:

```
(clj-insim/enqueue! lfs-client (packets/mtc "Hello, world!"))
```

To stop the client:
```
(clj-insim/stop! lfs-client)
```

### Maintaining the connection

You can supply a dispatch function. It should accept a packet and return something (nil, a packet or a collection of packets).

```
(defmulti dispatch #(get-in % [::packet/header :type]))

(defmethod dispatch :default [packet]
  (println packet)) ;; Print incoming packets

(defmethod dispatch :tiny [_]
  (packets/tiny)) ;; Return a tiny packet as a response to an incoming tiny packet
  
(def lfs-client (clj-insim/client dispatch))
```

## License

Copyright © 2020 Stan Verberkt

Distributed under the Unlicense License.
This is free and unencumbered software released into the public domain.
For more information, please refer to <http://unlicense.org>
