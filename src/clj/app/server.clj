(ns app.server
  (:require
   [clojure.string :as string]
   [io.pedestal.connector :as conn]
   [io.pedestal.log :refer [debug info spy]]
   [io.pedestal.http.http-kit :as hk]))

(defonce submissions (atom {}))

;; TODO: Implement
(defn submission-verification [{:keys [path-params]}]
  (debug :prose "verifying submission" :submissions @submissions :path-params path-params)
  (if-let [submission-uuid (parse-uuid (:submission-uuid path-params))]
    (do
      (debug :prose "verifying submission id"  :submission-uuid submission-uuid)
      (if-let [submission (get @submissions submission-uuid)]
        (do
          (debug :prose "found submission" :submission submission)
          ;; TODO: Simplify this hack
          (eval `(info ~@(flatten (into [] submission))))
          (spy {:status  200
                :headers {"Content-Type" "text/plain"}
                :body  "Thank you for confirmation. Your form is delivered."}))
        (spy {:status  404
              :headers {"Content-Type" "text/plain"}
              :body    "Submission not found"})))
    (spy {:status  422
          :headers {"Content-Type" "text/plain"}
          :body    "Invalid submission uuid"})))

(defn form-handler
  [{:keys [params]}]
  (debug :prose "form received" :params params)
  (let [email   (get params "email")]
    (if-not (string/blank? email)
      (let [submission-uuid  (random-uuid)
            confirmation-url (str "http://localhost:8080/confirm-submission/" submission-uuid)]
        (info :prose "valid form submitted" :by email)
        (swap! submissions assoc submission-uuid params)
        (info :prose "sending confirmation link"
              :confirmation-url confirmation-url
              :submission-uuid submission-uuid)
        (spy {:status  200
              :headers {"Content-Type" "text/plain"}
              :body    (str "Thank you for sending the form. We have sent you an email with confirmation link to " email)}))
      (do
        (info :prose "Missing required field" :field "email")
        (spy {:status  422
              :headers {"Content-Type" "text/plain"}
              :body    "Missing required field email"})))))

(defn home-handler
  [_request]
  (spy {:status  200
        :headers {"Content-Type" "text/plain"}
        :body "Hello, form!"}))

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
  (info :prose "Starting Form to Mail" :host host :port port)
  connector-map)

(defn create-connector
  []
  (-> (conn/default-connector-map "0.0.0.0" 8080)
      (conn/with-default-interceptors)
      (conn/with-routes routes)
      (log-connector)
      (hk/create-connector nil)))

;; For interactive development
(defonce *connector (atom nil))

(defn start []
  (reset! *connector
          (conn/start! (create-connector))))

(defn stop []
  (conn/stop! @*connector)
  (reset! *connector nil))

(defn restart []
  (stop)
  (start))

