# clj-insim

A Clojure library designed to interface with Live For Speed InSim.

## Usage

Start an LFS client with the default parser, on localhost and port 29999.

```
(ns something.core
  (:require [clj-insim.core :as clj-insim]))

(def client (clj-insim/start-client))
```

Stop the client by resetting the atom to false.
```
(reset! client false)
```

## License

Copyright © 2018 Stan Verberkt

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
