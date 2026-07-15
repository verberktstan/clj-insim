(ns build
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
(def uber-file "target/aiskill.jar")
(def target-dir "target")
(def dist-dir "target/dist")

(def ^:private aiskill-tag-prefix
  "Prefix for aiskill's own release tags, kept separate from clj-insim's
   library version tags (e.g. v0.3.1)."
  "aiskill-v")

(def ^:private aiskill-tag-pattern
  (re-pattern (str "^" aiskill-tag-prefix "(\\d+)\\.(\\d+)$")))

(defn- dirty?
  "True when the working tree has uncommitted changes."
  []
  (not (str/blank? (b/git-process {:git-args "status --porcelain"}))))

(defn- next-aiskill-version
  "Finds existing aiskill-vMAJOR.MINOR tags and returns the next version
   string, e.g. `0.2`. Bumps the minor version of the highest existing tag,
   starting at `0.1` when no aiskill tags exist yet. To bump the major
   version, tag a release by hand, e.g. `git tag aiskill-v1.0`."
  []
  (let [tags (->> (or (b/git-process {:git-args ["tag" "--list" (str aiskill-tag-prefix "*")]}) "")
                   str/split-lines
                   (remove str/blank?))
        versions (keep #(when-let [[_ major minor] (re-matches aiskill-tag-pattern %)]
                           [(Integer/parseInt major) (Integer/parseInt minor)])
                        tags)
        [major minor] (or (last (sort versions)) [0 0])]
    (str major "." (inc minor))))

(defn- tag! [tag-name]
  (b/git-process {:git-args ["tag" tag-name]}))

(defn- untag! [tag-name]
  (b/git-process {:git-args ["tag" "-d" tag-name]}))

(defn- runner-script? [filename]
  (or (str/ends-with? filename ".bat") (str/ends-with? filename ".sh")))

(defn- zip-target!
  "Stages the built jar, compiled classes and runner scripts into a versioned
   folder and zips it, so the whole target folder's build output can be
   shipped as a single file."
  [version]
  (b/delete {:path dist-dir})
  (let [dist-name (str "aiskill-" version)
        stage-dir (str dist-dir "/" dist-name)]
    (println "  - staging files")
    (b/copy-file {:src uber-file :target (str stage-dir "/aiskill.jar")})
    (b/copy-dir {:src-dirs [class-dir] :target-dir (str stage-dir "/classes")})
    (b/copy-file {:src "LICENSE" :target (str stage-dir "/LICENSE")})
    (b/copy-file {:src "docs/aiskill-readme.md" :target (str stage-dir "/README.md")})
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
  (when (dirty?)
    (throw (ex-info "Working tree has uncommitted changes; commit or stash before building aiskill." {})))
  (let [version (next-aiskill-version)
        tag (str aiskill-tag-prefix version)]
    (println " - tagging" tag)
    (tag! tag)
    (try
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
        (zip-target! version))
      (println (str " - tagged " tag " (run 'git push origin " tag "' to publish)"))
      (catch Throwable t
        (println " - build failed, removing tag" tag)
        (untag! tag)
        (throw t)))))
