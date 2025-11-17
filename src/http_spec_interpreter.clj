(ns http-spec-interpreter
  (:require
   [clojure.data.json :as json]
   [clojure.string :refer [blank?]]
   ;; [clojure.pprint :refer [pprint]]
   ))

(println (json/write-str {:type "InterpreterState"
                          :ready true}))
(doseq [line (line-seq (java.io.BufferedReader. *in*))]
  (when-not (blank? line)
    (.println *err* (json/read-str line :key-fn keyword))
    (println (json/write-str {:type "Success"}))))
