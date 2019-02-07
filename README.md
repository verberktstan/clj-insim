# clj-insim

A Clojure library designed to interface with [Live For Speed](https://www.lfs.net/) [InSim](https://en.lfsmanual.net/wiki/InSim).

You can find it on Clojars: https://clojars.org/clj-insim/

## Usage

Add clj-insim to your project.clj:
```
[clj-insim "0.1.5-SNAPSHOT"]
```

Require the clj-insim.core ns:
```
(ns my-project.core
  (:require [clj-insim.core :as clj-insim]))
```

### Very basic default client

Make sure to start InSim from LFS by executing `/insim 29999`.
To set up a client with the default handler:
```
(def lfs-client (clj-insim/client))
```

Start a new server in LFS and add an AI player and a human player. The host machine should be notified of the newly registered players by default. To inspect the connections/players in the clj-insim client:
```
@clj-insim.connection/connections
@clj-insim.player/players
```

To stop the client:
```
(reset! lfs-client false)
```

### Create your own packet handler

Include the `clj-insim/packets` ns and define a basic multimethod for dispatching packets. You can dispatch based on the `:type` of packet. *Make sure it returns nil by default*. If you want to send data back to lfs, always return a seq with packets (even if there is only one packet!).
Below, a dispatch fn for a `{:type :mso}` (IS_MSO) packet is defined, which simply prints the incoming packet to the repl.
The handler accepts a packet from LFS and *must* return a valid packet. Note the dispatching of `{:type :tiny}` packets, returning a (coll with a) basic IS_TINY to maintain connection.

Most of the time I make sure the dispatch fns return a valid packet OR nil. This way you can easily fallback on a default `(packets/is-tiny)` packet (the default maintain connection packet).
```
(ns my-project.core
  (:require [clj-insim.core :as clj-insim]
            [clj-insim.packets :as packets]))
            
(defmulti dispatch :type)
(defmethod dispatch :default [_] nil)

(defmethod dispatch :mso [p]
  (newline)
  (prn p))
  
(defmethod dispatch :tiny [tiny-packet]
  (when (clj-insim/keep-alive-packet? tiny-packet)
    ;; Return a IS_TINY packet to maintain connection
    [(packets/is-tiny)])) ; Note the explicit wrapping in a vector/coll
  
(defn handler [{:keys [type] :as packet}]
  (seq (dispatch packet))) ;; Always return a seq (or nil)
  
(comment
  (def lfs-client (clj-insim/client handler))
  (reset! lfs-client false)
)
```
If you start a client with this handler and type some message in LFS, you'll see it printed in the repl.
This is nice, but it would be even nicer if we send something useful back to LFS!

### Returning a packet from your dispatch fn
Don't forget that you should always return a coll (or nil) from a dispatch fn.
Replace the previous :mso dispatch function with this one:
```
(defn mso-prefix? [{:keys [type user-type]}]
  (and (= type :mso) (= user-type :prefix)))

(defmethod dispatch :mso [{:keys [text-start message uniq-connection-id] :as mso-packet}]
  (when (mso-prefix? mso-packet) ;; Only react to message with the insim prefix (! by default)
    (let [command (subs message text-start)] ;; Take a substring (the command)
      (case command
        "!help"
        [(packets/is-mtc uniq-connection-id 0 "You asked for help")] ;; Message to connection

        [(packets/is-msl "Unkwown command!")] ;; Message to appear on local computer only
))))
```
Type `!help` and `!unknown` in LFS and you should get the reaction you expect from clj-insim.

### Connections & players
If you want to use the built-in dispatchers for New ConnectioN, ConnectioN Left, New PLayer & PLayer Left packets, specify the dispatchers for these packets like this:
```
(defmethod dispatch :ncn [p] (connection/dispatch-ncn p {:notify-host? true})) ; you can omit :notify-host? 
(defmethod dispatch :cnl [p] (connection/dispatch-cnl p {:notify-host? true}))
(defmethod dispatch :npl [p] (player/dispatch-npl p {:notify-host? true}))
(defmethod dispatch :pll [p] (player/dispatch-pll p {:notify-host? true}))
```
If a user connected or player joined, it is automatically registered. You can inspect `@clj-insim.connection/connections` to see the current connections and `@clj-insim.player/players` to see the current players in the game.

## License

Copyright © 2018 Stan Verberkt

Distributed under the Unlicense License.
This is free and unencumbered software released into the public domain.
For more information, please refer to <http://unlicense.org>
