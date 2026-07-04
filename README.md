# clj-insim

A clojure library to connect with Live For Speed racing simulator via the InSim protocol.

![Tests](https://github.com/verberktstan/clj-insim/actions/workflows/clojure.yml/badge.svg)

***Pre 0.2.1 deprecation warning***

> clj-insim 0.2.1-SNAPSHOT (and versions before that) are still available on Clojars. These are deprecated and not maintained!
> This repo contains version 0.3.x with a completely different async architecture.

## Version Support

clj-insim supports **InSim Protocol v10** (LFS 0.8B and later).

- **Protocol Version**: v10 (default)
- **Live For Speed**: 0.8B and later
- **Backward Compatibility**: Handles v9 packets transparently (time values automatically converted)

The library automatically negotiates the protocol version with LFS and adapts time handling:
- **v10**: All time fields in milliseconds
- **v9**: Time values converted from hundredths of seconds to milliseconds for consistency

## Configuration

Include a dependency on this project and core.async in your `deps.edn`.

```clojure
;; v 0.3.1
:deps {com.github.verberktstan/clj-insim {:git/tag "v0.3.1"
                                          :git/sha "df28ecf"}}
```

## Printing incoming packets

Require clj-insim.client in your ns.

```clojure
(ns core
  (:require [clj-insim.client :as client]))
```

Define a function that starts the client, using client/start and client/go.

```clojure
(defn start-listener []
  ;; Start the client
  (let [client (client/start)]
    ;; Start a async go-loop that simply prints packets
    ;; The dispatch function supplied to clj-insim.client/go should accept 2
    ;; arguments, the client and incoming the packet.
    (client/go client (fn [_ packet]
                        (println packet)))
    ;; Return the client map, so we can stop it later.
    client))

(comment
  ;; To start the client:
  (def lfs-client (start-listener))

  ;; To stop it:
  (client/stop lfs-client)
)
```

## Documentation
[Take a look at the docs](https://htmlpreview.github.io/?https://github.com/verberktstan/clj-insim/blob/552993f18a4781d148f00628077521822d6ce66d/target/doc/index.html)

## Run examples from cli

I develop this on Mac OSX and Linux, and didn't test all this on Windows. Please refer to [Getting Started - Installation on Windows](https://clojure.org/guides/getting_started#_installation_on_windows) to get it going.

Make sure clojure is installed and run one of the examples by executing:

```
clojure -m examples.safety
```
```
clojure -m examples.scoring
```
```
clojure -m examples.buttons
```

Take a look at safety.clj, scoring.clj & buttons.clj in the src/examples directory to see how these example clients are implemented.
