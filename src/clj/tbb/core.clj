(ns tbb.core
  (:require
   [clojure.data.json :as json]
   [clojure.string :refer [blank?]]
   [io.pedestal.log :refer [info]]))


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
            variant       (get-in message [:step :variant])
            arguments     (get-in message [:step :arguments])
            implmentation (get @steps-implementation variant)]
        (info :prose "got a message from tbb" :message message)
        (if (nil? implmentation)
          (println (json/write-str {:type   "Failure"
                                    :reason "Not implemented"}))
          (try
            (apply implmentation arguments)
            (println (json/write-str {:type "Success"}))
            (catch AssertionError e
              (println (json/write-str {:type   "Failure"
                                        :reason (.getMessage e)}))))))))

  (info :prose "Done reading from tbb."))
