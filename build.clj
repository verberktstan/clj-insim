(ns build
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
(def uber-file "target/aiskill.jar")
(def target-dir "target")
(def dist-dir "target/dist")

(defn- version
  "Derives a version string from git, e.g. `v0.3.1-4-gb5c8d5b`, appending
   `-dirty` when there are uncommitted changes. Falls back to the commit's
   short SHA if no tags exist yet."
  []
  (str/trim (b/git-process {:git-args "describe --tags --always --dirty"})))

(defn- runner-script? [filename]
  (or (str/ends-with? filename ".bat") (str/ends-with? filename ".sh")))

(defn- zip-target!
  "Stages the built jar, compiled classes and runner scripts into a versioned
   folder and zips it, so the whole target folder's build output can be
   shipped as a single file."
  []
  (b/delete {:path dist-dir})
  (let [dist-name (str "aiskill-" (version))
        stage-dir (str dist-dir "/" dist-name)]
    (println "  - staging files")
    (b/copy-file {:src uber-file :target (str stage-dir "/aiskill.jar")})
    (b/copy-dir {:src-dirs [class-dir] :target-dir (str stage-dir "/classes")})
    (doseq [script (->> (io/file target-dir)
                        .listFiles
                        (filter #(-> (.getName %) runner-script?)))]
      (b/copy-file {:src (.getPath script) :target (str stage-dir "/" (.getName script))}))
    (println "  - zipping" dist-name)
    (b/zip {:src-dirs [dist-dir]
            :zip-file (str target-dir "/" dist-name ".zip")})
    (b/delete {:path dist-dir})))

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
  (let [basis (b/create-basis {:project "deps.edn"})
        examples-dir (io/file class-dir "examples")]
    (b/copy-dir {:src-dirs ["src"] :target-dir class-dir})
    ;; Keep only aiskill example source, remove all others
    (when (.exists examples-dir)
      (doseq [file (.listFiles examples-dir)]
        (let [name (.getName file)]
          (when (and (.endsWith name ".clj") (not= name "aiskill.clj"))
            (b/delete {:path (.getPath file)})))))
    (println " - compiling clojure")
    (b/compile-clj {:basis basis :src-dirs ["src"] :class-dir class-dir})
    ;; Remove compiled classes for examples other than aiskill
    (when (.exists examples-dir)
      (doseq [file (.listFiles examples-dir)]
        (let [name (.getName file)]
          (when (and (.isFile file)
                     (.endsWith name ".class")
                     (not (str/starts-with? name "aiskill")))
            (b/delete {:path (.getPath file)})))))
    (println " - creating artifact" uber-file)
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :basis basis
             :main 'examples.aiskill})
    (println " - copying runner scripts")
    (copy-runner-script! "scripts/run-aiskill.sh")
    (copy-runner-script! "scripts/run-aiskill.bat")
    (println " - zipping target")
    (zip-target!)))
