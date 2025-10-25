(ns app.core
  (:require
   [app.server :as server])
  (:gen-class))

(defn -main [& args]
  (server/start))
