# clj-insim

A Clojure library designed to interface with [Live For Speed](https://www.lfs.net/) [InSim](https://en.lfsmanual.net/wiki/InSim).

## Usage

Add clj-insim to your project.clj:
```
[clj-insim "0.1.0-SNAPSHOT"]
```

Require clj-insim.core namespace
```
(ns something.core
  (:require [clj-insim.core :refer [start-test-client]]))
```

Be sure to run LFS on your localhost and run `/insim 29999` to setup the TCP server from within LFS.

Start an LFS client with the default test handler as defined in `clj-insim.core`.
```
(def lfs-client (start-test-client))
```

If you're running LFS 0.6T you should see a welcome message in LFS! If you're running another version, the connection should be closed automatically.
Inspect `clj-insim.core` to see how the test-handler and dispatch functions work.

Stop the client by resetting the atom to false.
```
(reset! lfs-client false)
```

## License

Copyright © 2018 Stan Verberkt

Distributed under the Unlicense License.
This is free and unencumbered software released into the public domain.
For more information, please refer to <http://unlicense.org>
