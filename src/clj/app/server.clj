(ns app.server
  (:require
    [aleph.http :as http]))


(defn handler
  [req]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body "Hello form, we love Nix!"})


(defn start
  []
  (http/start-server handler {:port 8080}))

