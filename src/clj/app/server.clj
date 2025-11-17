(ns app.server
  (:require
   [clojure.string :as string]
   [io.pedestal.connector :as conn]
   [clojure.pprint :refer [pprint]]
   [io.pedestal.http.http-kit :as hk]))

(defonce submissions (atom {}))


;; TODO: Implement
(defn submission-verification [{:keys [path-params]}]
  (pprint @submissions)
  (if-let [submission-uuid (parse-uuid (:submission-uuid path-params))]
    (do
      (println "Verifying submission id" submission-uuid)
      (if-let [submission (get @submissions submission-uuid)]
        (do
          (doseq [param (dissoc submission "email")]
            (println (str (key param) ": " (val param)))
            submission)
          {:status  200
           :headers {"Content-Type" "text/plain"}
           :body  "Thank you for confirmation. Your form is delivered."})
        {:status  404
         :headers {"Content-Type" "text/plain"}
         :body    "Submission not found"}))
    {:status  422
     :headers {"Content-Type" "text/plain"}
     :body    "Invalid submission uuid"}))

(defn form-handler
  [{:keys [params]}]
  (let [email   (get params "email")]
    (if-not (string/blank? email)
      (let [submission-uuid (random-uuid)]
        (println (str "Form submitted by " email))
        (swap! submissions assoc submission-uuid params)
        (println (str "Sending confirmation link: http://localhost:8080/confirm-submission/" submission-uuid))
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
  #{["/"
     :get home-handler
     :route-name :home]
    ["/poc-submit"
     :post form-handler
     :route-name :form-submit]
    ["/confirm-submission/:submission-uuid"
     :get submission-verification
     :route-name :submission-verification]})

(defn log-connector [{:keys [host port] :as connector-map}]
  (-> (str "Starting Form to Mail on " host ":" port)
      (#(.println *err* %)))
  connector-map)

(defn create-connector
  []
  (-> (conn/default-connector-map "0.0.0.0" 8080)
      (conn/with-default-interceptors)
      (conn/with-routes routes)
      (log-connector)
      (hk/create-connector nil)))


(defn start
  []
  (conn/start! (create-connector)))
