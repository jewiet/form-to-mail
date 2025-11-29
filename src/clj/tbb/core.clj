(ns tbb.core
  (:require
   [clojure.data.json :as json]
   [clojure.pprint :refer [pprint]]
   [clojure.string :refer [blank?]]
   [io.pedestal.log :refer [debug error info]]))


(defonce ^:private steps-implementation (atom {}))

(defn implement-step [variant f]
  (swap! steps-implementation assoc variant f))

;; See unit test for example uses
(defmacro tis
  "Similar to assert but the exception message will have arguments evaluated. See unit tests for more details"
  [f & args]
  `(when-not (~f ~@args)
     (throw (AssertionError. (str "failed assertion: "
                                  (cons '~f (list ~@args)))))))

(defn ready []
  (println (json/write-str {:type "InterpreterState"
                            :ready true}))

  (doseq [line (line-seq (java.io.BufferedReader. *in*))]
    (when-not (blank? line)
      (let [message       (json/read-str line :key-fn keyword)
            step          (:step message)
            variant       (:variant step)
            arguments     (:arguments step)
            implmentation (get @steps-implementation variant)]
        (debug :prose "got a message from tbb" :message message)
        (if (nil? implmentation)
          (let [suggestion
                `(tbb/implement-step
                  ~variant
                  (~'fn [~@(map-indexed
                            (fn [indx _]
                              (symbol (str "arg-" indx)))
                            arguments)
                         data]
                   (tis ~'= "cat" "dog")))]

            (error :missing-step-implemnetation variant)
            (println (json/write-str {:type   "Failure"
                                      :reason "Not implemented"
                                      :hint (str "To get started put this in your interpreter:\n\n"
                                                 "``` clojure\n"
                                                 (with-out-str (pprint suggestion))
                                                 "```")})))
          (try
            (apply implmentation (conj arguments step))
            (println (json/write-str {:type "Success"}))
            (catch Throwable e
              (println (json/write-str {:type   "Failure"
                                        :reason (.getMessage e)}))))))))

  (debug :prose "Done reading from tbb."))


