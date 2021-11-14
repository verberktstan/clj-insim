# clj-insim

A clojure library to connect with Live For Speed racing simulator via the InSim protocol.

![Tests](https://github.com/verberktstan/clj-insim/actions/workflows/clojure.yml/badge.svg)

***Pre 0.2.1 deprecation warning***

> clj-insim 0.2.1-SNAPSHOT (and versions before that) are still available on Clojars. These are deprecated and not maintained!
> This repo contains version 0.3.x with a completely different async architecture.

## Configuration

Include a dependency on this project and core.async in your `deps.edn`.

```clojure
;; v 0.3.0
:deps {com.github.verberktstan/clj-insim {:git/tag "v0.3.0"
                                          :git/sha "552993f"}}
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
[Take a look at the docs](https://htmlpreview.github.io/?https://github.com/verberktstan/clj-insim/blob/e9dde5c927fe797ad8308c97c38dc29ce9583030/target/doc/index.html)

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
