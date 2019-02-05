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

Start a new server in LFS and add an AI player and a human player. The host machine should be notified of the newly registered players by default. To inspect the players in the clj-insim client:
```
@clj-insim.player/players
```

To stop the client:
```
(reset! lfs-client false)
```

## License

Copyright © 2018 Stan Verberkt

Distributed under the Unlicense License.
This is free and unencumbered software released into the public domain.
For more information, please refer to <http://unlicense.org>
