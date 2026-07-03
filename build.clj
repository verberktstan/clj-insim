(ns build
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
(def uber-file "target/aiskill.jar")
(def target-dir "target")

(defn- copy-runner-script!
  "Copies `src` (a run-aiskill launcher script from scripts/, which refers
   to the jar via the [[::JAR-TARGET::]] placeholder) into `target-dir`,
   rewriting the placeholder to the jar's path relative to the script's new
   location alongside the jar."
  [src]
  (let [dest    (io/file target-dir (.getName (io/file src)))
        content (-> (slurp src)
                    (str/replace "[[::JAR-TARGET::]]" "aiskill.jar"))]
    (spit dest content)
    (.setExecutable dest true false)))

(defn aiskill [_]
  (b/delete {:path class-dir})
  (b/delete {:path uber-file})
  (let [basis (b/create-basis {:project "deps.edn"})]
    (b/copy-dir {:src-dirs ["src"] :target-dir class-dir})
    (b/compile-clj {:basis basis :src-dirs ["src"] :class-dir class-dir})
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :basis basis
             :main 'examples.aiskill})
    (copy-runner-script! "scripts/run-aiskill.sh")
    (copy-runner-script! "scripts/run-aiskill.bat")))
