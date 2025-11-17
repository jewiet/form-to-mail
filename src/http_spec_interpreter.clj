(ns http-spec-interpreter
  (:require
   [clojure.data.json :as json]
   [clojure.string :refer [blank?]]
   ;; [clojure.pprint :refer [pprint]]
   ))

(def steps-implementation
  {"Make a {0} request to {1}."
   (fn [method url] (.println *err* "running step"))})

(println (json/write-str {:type "InterpreterState"
                          :ready true}))

(doseq [line (line-seq (java.io.BufferedReader. *in*))]
  (when-not (blank? line)
    (let [message       (json/read-str line :key-fn keyword)
          variant       (get-in message [:step :variant])
          arguments     (get-in message [:step :arguments])
          implmentation (get steps-implementation variant)]
      (.println *err* message)
      (if (nil? implmentation)
        (println (json/write-str {:type   "Failure"
                                  :reason "Not implemented"}))
        (do (apply implmentation arguments)
            (println (json/write-str {:type "Success"})))))))
