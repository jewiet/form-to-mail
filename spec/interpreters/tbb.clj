(ns tbb
  (:require
   [cheshire.core :as json]
   [clojure.pprint :refer [pprint]]
   [clojure.string :refer [blank?]]
   [taoensso.timbre :as timbre]))

(timbre/merge-config!
 {:appenders
  {:println (timbre/println-appender
             {:stream *err*})}
  :min-level (keyword (or
                       (System/getenv "tbb_interpreter_log")
                       "info"))})

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
  (println (json/encode {:type "InterpreterState"
                         :ready true}))

  (doseq [line (line-seq (java.io.BufferedReader. *in*))]
    (when-not (blank? line)
      (let [message       (json/parse-string line true)
            step          (:step message)
            variant       (:variant step)
            arguments     (:arguments step)
            implmentation (get @steps-implementation variant)]
        (timbre/debug "got a message from tbb" message)
        (if (nil? implmentation)
          (let [suggestion
                `(tbb/implement-step
                  ~variant
                  (~'fn [~@(map-indexed
                            (fn [indx _]
                              (symbol (str "arg-" indx)))
                            arguments)
                         ~'data]
                        (tis ~'= "cat" "dog")))]

            (timbre/error "missing step implementation" variant)
            (println (json/generate-string {:type   "Failure"
                                            :reason "Not implemented"
                                            :hint (str "To get started put this in your interpreter:\n\n"
                                                       "``` clojure\n"
                                                       (with-out-str (pprint suggestion))
                                                       "```")})))
          (try
            (apply implmentation (conj arguments step))
            (println (json/generate-string {:type "Success"}))
            (catch Throwable e
              (println (json/generate-string {:type   "Failure"
                                              :reason (.getMessage e)}))))))))

  (timbre/debug "Done reading from tbb."))

(defn send-text [text]
  (println (json/generate-string {:type   "Text"
                                  :content (pr-str text)}))
  text)

(defn send-link
  ([url]
   (println (json/generate-string {:type "Link"
                                   :url  (str url)}))
   url)

  ([url label]
   (println (json/generate-string {:type  "Link"
                                   :url   (str url)
                                   :label label}))
   url))

(defn table->maps [table]
  ;; Each column in a table becomes a map in an array. Keys are derived from the first row.
  (let [[header & rows] table
        keywords        (map keyword header)]
    (map #(zipmap keywords %) rows)))

(defn table->map
  "Takes a sequence of maps with :name and :value pairings and returns a single map"
  [maps key-kw value-kw]
  (let [coll   (table->maps maps)
        ks     (map #(keyword (key-kw %)) coll)
        values (map value-kw coll)]
    (zipmap ks values)))
