# clj-insim

A clojure library to connect with Live For Speed racing simulator via the InSim protocol.

![Tests](https://github.com/verberktstan/clj-insim/actions/workflows/clojure.yml/badge.svg)

At the time of writing, you can only run it from clojure directly. Knowledge of clojure and the repl is assumed.
I'll try to deploy the clj-insim client to clojars soon, so you can use it as a dependency easily.

## Run examples from cli

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
