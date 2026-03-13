(ns tbb.core
  (:require
   [clojure.data.json :as json]
   [clojure.pprint :refer [pprint]]
   [clojure.string :refer [blank?]]
   [clojure.tools.logging.readable :as logging]))

;; TODO: Make it work with Babashka (so no dependency on JDK)
;; TODO: Distribute this with TBB so it can be used in other Clojure projects.

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
        (logging/debug "got a message from tbb" message)
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

            (logging/error "missing step implementation" variant)
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

  (logging/debug "Done reading from tbb."))
