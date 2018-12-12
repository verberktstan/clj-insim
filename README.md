# clj-insim

A Clojure library designed to interface with Live For Speed InSim.

## Usage

Add clj-insim to your project.clj:
```
[clj-insim "0.1.0-SNAPSHOT"]
```

Start an LFS client with the default parser (by default on localhost with port 29999).
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
