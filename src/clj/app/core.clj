(ns app.core
  (:gen-class)
  (:require
   [app.server :as server]))

(defn -main
  "Form to Mail app"
  [& args]
  (let [config-file (first args)
        config      (read-string (slurp config-file))]
    (server/start config)))
