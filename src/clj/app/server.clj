(ns app.server
  (:require
   [aleph.http :as http]
   [clojure.string :as string]
   [ring.middleware.params :refer [wrap-params]]))



(defn form-handler
  [{:keys [params]}]
  (let [message (get params "message")
        email   (get params "email")]

    (if-not (string/blank? email)
      (do (println (str "Form submitted by " email))
          (println (str "message: " message))
          {:status  200
           :headers {"Content-Type" "text/html"}
           :body    "Hello form,  We love Nix!"})
      (do
        (println "Missing required field email")
        {:status  200
         :headers {"Content-Type" "text/html"}
         :body  "Missing required field email"}))))

(def http-handler
  (-> form-handler
      wrap-params))

(defn start
  []
  (http/start-server http-handler {:port 8080}))

