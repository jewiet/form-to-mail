(ns app.server
  (:require
   [aleph.http :as http]
   [clojure.string :as string]
   [clojure.pprint :refer [pprint]]
   [ring.middleware.params :refer [wrap-params]]))



(defn form-handler
  [{:keys [params]}]
  (let [message (get params "message")
        email   (get params "email")]

    (if-not (string/blank? email)
      (do (println (str "Form submitted by " email))
          (println (str "message: " message))
          {:status  200
           :headers {"Content-Type" "text/plain"}
           :body    (str "Thank you for sending the form. We have sent you an email with confirmation link to " email)})
      (do
        (println "Missing required field email")
        {:status  422
         :headers {"Content-Type" "text/plain"}
         :body  "Missing required field email"}))))

(defn home-handler
  [_request]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body "Hello, form!"})

(defn router
  [request]
  (let [route (select-keys request [:uri :request-method])]
    (case route
      {:uri "/"
       :request-method :get} (home-handler request)
      {:uri "/poc-submit"
       :request-method :post} (form-handler request)
      {:status  404
       :headers {"Content-Type" "text/plain"}
       :body "Not Found"})))

(def http-handler
  (-> router
      wrap-params))

(defn start
  []
  (http/start-server http-handler {:port 8080}))

