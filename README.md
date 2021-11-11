# clj-insim

A clojure library to connect with Live For Speed racing simulator via the InSim protocol.

![Tests](https://github.com/verberktstan/clj-insim/actions/workflows/clojure.yml/badge.svg)

***Pre 0.2.1 deprecation warning***

clj-insim 0.2.1-SNAPSHOT (and versions before that) are still available on Clojars. These are deprecated and not maintained!
This repo contains version 0.3.x (and up) with a (completely different) async architecture.

At the time of writing, you can only run it from clojure directly. Knowledge of clojure and the repl is assumed.
I'll try to deploy the clj-insim 0.3.x client to clojars soon, so you can use it as a dependency easily.

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
