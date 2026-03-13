(ns interpreters.common
  (:require
   [clojure.string :as string]
   [io.pedestal.log :refer [debug error info]]))

(defn read-log-file [log-file]
  (->> log-file
       str
       slurp
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

;; Make a smart-spy that takes the level
;; FIXME: Line number in output always points here instead of call site
(defn info-spy [prose value]
  (info :prose prose :value value)
  value)

(defn debug-spy [prose value]
  (debug :prose prose :value value)
  value)

(defn wait-for-log [pattern log-file]
  (loop [iteration 0]
    (debug :iteration iteration)

    ;; Repeat until the pattern is found...
    (when-not (as-> log-file $
                (read-log-file $)
                (has-matching? pattern $))
      ;; ...but no more than n times
      (when (> iteration 250)
        (error :log-not-found pattern :logs (read-log-file log-file))
        (throw (ex-info "Timeout exceeded" {})))

      (Thread/sleep 100)
      (recur (inc iteration)))))
