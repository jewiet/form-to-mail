(ns app.core
  (:gen-class)
  (:require
    [app.server :as server]))

(defn -main
  "Form to Mail app"
  [& args]
  (server/start))
