(ns common
  (:require
   [clojure.string :as string]
   [tbb]
   [taoensso.timbre :as logging]))

(defn read-log-file [log-file]
  (->> log-file
       str
       slurp
       (tbb/send-snippet "logs" {:caption (str "Logs from " log-file)})
       string/split-lines
       (keep #(re-find #"\{.*\}" %))
       (map read-string)
       doall))

(defn map-includes? [small big]
  (->> small
       keys
       (select-keys big)
       (= small)))

(defn filter-matching [small coll]
  (filter #(map-includes? small %) coll))

(defn find-matching [small coll]
  (->> coll
       (filter-matching small)
       (first)))

(defn has-matching?
  "Takes a small map and a collection of maps, checks if any matches"
  [small coll]
  (not-empty (find-matching small coll)))

(defn wait-for-log [pattern log-file]
  (tbb/send-link log-file "Log file")
  (loop [iteration 0]
    (logging/debug "Wait iteration" iteration)

    ;; Repeat until the pattern is found...
    (when-not (as-> log-file $
                (read-log-file $)
                (has-matching? pattern $))
      ;; ...but no more than n times
      (when (> iteration 250)
        (logging/error "Log not found" {:pattern pattern :logs (read-log-file log-file)})
        (throw (ex-info "Timeout exceeded" {})))

      (Thread/sleep 100)
      (recur (inc iteration)))))
