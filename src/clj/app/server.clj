(ns app.server
  (:require
    [clojure.string :as string]
    [io.pedestal.connector :as conn]
    [io.pedestal.http.http-kit :as hk]))


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
         :body    "Missing required field email"}))))


(defn home-handler
  [_request]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body "Hello, form!"})


(def routes
  #{["/" :get home-handler :route-name :home]
    ["/poc-submit" :post form-handler :route-name :form-submit]})


(defn create-connector
  []
  (-> (conn/default-connector-map "0.0.0.0" 8080)
      (conn/with-default-interceptors)
      (conn/with-routes routes)
      (hk/create-connector nil)))


(defn start
  []
  (conn/start! (create-connector)))
