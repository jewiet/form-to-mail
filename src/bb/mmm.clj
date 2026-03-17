(ns mmm
  (:require
   [babashka.fs :as fs]))

(defn build
  "Takes a path pointing to a result directory and a map"
  [out-path website]
  (let [absolute-path (fs/absolutize out-path)]
    (println (str "Writing to " absolute-path "..."))
    (fs/create-dirs absolute-path)
    (doall (for [[path value] website]
             (let [normalized-path (fs/normalize path)
                   absolute-path (fs/path absolute-path normalized-path)]
               (when (fs/absolute? normalized-path)
                 (throw (ex-info (str "Paths can't be absolute. Can't have " path)
                                 {:babshka/exit 1})))
               (when (fs/starts-with? normalized-path "..")
                 (throw (ex-info (str "Paths can't point to a parent directory. Can't have " path)
                                 {:babshka/exit 1})))
               (cond
                 (instance? java.nio.file.Path value)
                 (if (fs/directory? value)
                   (fs/copy-tree value absolute-path
                                 {:replace-existing true})
                   (do

                     (when-not (fs/exists? (fs/parent absolute-path))
                       (fs/create-dirs (fs/parent absolute-path)))
                     (fs/copy value absolute-path
                              {:replace-existing true})))

                 (string? value)
                 (do (when-not (fs/exists? (fs/parent absolute-path))
                       (fs/create-dirs (fs/parent absolute-path)))
                     (spit (str absolute-path) value))

                 (map? value)
                 (build absolute-path value)

                 :else
                 (println "Cant handle" (type value) "at" path)))))))
