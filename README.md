# clj-insim

A Clojure library designed to interface with [Live For Speed](https://www.lfs.net/) via the [InSim protocol](https://en.lfsmanual.net/wiki/InSim).

You can find it on Clojars: [![Clojars Project](https://img.shields.io/clojars/v/clj-insim.svg)](https://clojars.org/clj-insim)

## Example project

Check out [multiclass-racing](https://github.com/verberktstan/multiclass-racing), an example project using the clj-insim library!

## Usage

Add clj-insim to your project.clj:
```
[clj-insim "0.2.0-SNAPSHOT"]
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

To send a message to LFS, call `enqueue!` and pass the client and a MST (message type) or MTC (message to connection) packet:

```
(clj-insim/enqueue! lfs-client (packets/mst "Hello, world!"))
(clj-insim/enqueue! lfs-client (packets/mtc "Hello, world!"))
```

To stop the client:
```
(clj-insim/stop! lfs-client)
```

### Maintained connection

clj-insim automatically maintains the connection for you as long as the client isn't stopped.

### Connections and players

When a new connection joins the game, it will automatically be stored. Access this data by requesting a specific connection. If you don't specify a unique connection id, it will return a map of all registered connections.
```
(clj-insim/get-connection 0)
(clj-insim/get-connection)
```

The same is true for players.
```
(clj-insim/get-player 1)
(clj-insim/get-player)
```

### Dispatching

You can supply a dispatch function. It should accept a packet and return something (nil, a packet or a collection of packets). Dispatch on the packet type with the function `clj-insim/packet-type`, which returns the packet type, e.g. `:tiny` or `:res`.

If you return a packet (or coll of packets) this will automatically be enqueued on the out-queue and send to LFS. It's ok to return data that is not a valid InSim packet, this will be ignored by clj-insim.

The following dispatcher returns an MTC packet reporting the player's nickname and LFS username and the finish position (race result);
```
(defmulti dispatch clj-insim/packet-type)

(defmethod dispatch :default [packet]
  (println packet)) ;; Print incoming packets

(defmethod dispatch :res [{::packet/keys [header body]}]
  (let [{:keys [user-name player-name result-num]} body]
    (packets/mtc (str player-name " (" user-name ") got a result: " (inc result-num)))))
  
(def lfs-client (clj-insim/client dispatch))
```

## License

Copyright © 2020 Stan Verberkt

Distributed under the Unlicense License.
This is free and unencumbered software released into the public domain.
For more information, please refer to <http://unlicense.org>
