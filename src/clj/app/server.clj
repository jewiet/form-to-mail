(ns app.server
  (:require
   [clojure.string :as string]
   [io.pedestal.connector :as conn]
   [io.pedestal.log :refer [debug info spy]]
   [io.pedestal.http.http-kit :as hk]))

(defonce configuration (atom nil))

(defonce submissions (atom {}))

(defn send-mail [from to subject body]
  (info :prose "sending an email"
        :to to
        :reply-to from
        :subject subject
        ;; Use a templating library
        :body body))

;; TODO: Implement
(defn submission-verification [{:keys [path-params]}]
  (debug :prose "verifying submission" :submissions @submissions)
  (if-let [submission-uuid (parse-uuid (:submission-uuid path-params))]
    (do
      (debug :prose "verifying submission id"  :submission-uuid submission-uuid)
      (if-let [submission (get @submissions submission-uuid)]
        (do
          (debug :prose "found submission" :submission submission)
          (send-mail (:email submission)
                     (:receiver submission)
                     "Form to Mail message"
                     (:message submission))
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
  [{:keys [form-params path-params]}]
  (let [email       (:email form-params)
        receiver-id (:receiver-id  path-params)
        receiver    (get-in @configuration [:receivers receiver-id])]
    (if (nil? receiver)
      (spy {:status  404
            :headers {"Content-Type" "text/plain"}
            :body    "No such receiver"})
      (if-not (string/blank? email)
        (let [submission-uuid  (random-uuid)
              confirmation-url (str "http://localhost:8080/confirm-submission/" submission-uuid)]
          (info :prose "valid form submitted" :by email)
          (swap! submissions assoc submission-uuid
                 (assoc form-params :receiver receiver))
          (send-mail "info@form-to-mail.com"
                     email
                     "Form to Mail confirmation"
                     (str "Please <a href='" confirmation-url "'>confirm your submission</a>"))
          (spy {:status  200
                :headers {"Content-Type" "text/plain"}
                :body    (str "Thank you for sending the form. We have sent you an email with confirmation link to " email)}))
        (do
          (info :prose "Missing required field" :field "email")
          (spy {:status  422
                :headers {"Content-Type" "text/plain"}
                :body    "Missing required field email"}))))))

(defn home-handler
  [_request]
  (spy {:status  200
        :headers {"Content-Type" "text/plain"}
        :body "Hello, form!"}))

(def routes
  #{["/"
     :get home-handler
     :route-name :home]
    ["/submit/:receiver-id"
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

(defn start [config]
  (reset! configuration config)
  (reset! *connector
          (conn/start! (create-connector))))

(defn stop []
  (conn/stop! @*connector)
  (reset! *connector nil))

(defn restart []
  (stop)
  (start @configuration))
