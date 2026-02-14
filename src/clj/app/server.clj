(ns app.server
  (:require
   [clojure.string :as string]
   [io.pedestal.connector :as conn]
   [io.pedestal.log :refer [debug info spy]]
   [io.pedestal.http.http-kit :as hk]
   [io.pedestal.interceptor :as interceptor]
   [postal.core :as postal]
   [babashka.fs :as fs]))


(defn create-html [body]
  (str "<html><head> </head><body> <dl style='font-size: 0.9rem; 'margin-bottom: 1em'> Contents of the form submitted"
       (clojure.string/join (map (fn [[k v]]
                                   (str "<dt style='font-weight:bold;'>" (name k)
                                        "</dt><dd style='margin-bottom: 1em; margin-left: 0.5rem; font-size: 0.9rem; '>" v "</dd>"))
                                 body))
       "</dl></body></html>" ))


(defonce configuration (atom nil))

(defonce submissions (atom {}))

(defn send-mail
  "Send an email.

  If config has :smpt-server key, use its value. Otherwise log details of the
  message for mocking.

  The first argument (reply-to) can be nil if the message shouldn't be replied
  to."
  [reply-to to subject body]
  (let [raw-body-file (str (fs/create-temp-file {:prefix "form-to-mail-raw-body"
                                                 :suffix ".txt"}))]
    (when (map? body)
      (spit raw-body-file (:raw-body body)))
   (if-let [smtp-config (:smtp-server @configuration)]
     (postal/send-message smtp-config
                         {:from     (:from-address @configuration)
                          :reply-to reply-to
                          :to       to
                          :subject  subject
                          :body  [{:type "text/html"
                                   :content (if (map? body)
                                              (create-html (dissoc body :raw-body))
                                              body)}
                                  {:type :attachment
                                   :content raw-body-file}]})
    ;; TODO: DRY
    (info :prose "sending an email"
          :from (:from-address @configuration)
          :to to
          :reply-to reply-to
          :subject subject
          :body body))))

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
                     ;; Use a templating library
                     submission)

          ;; TODO: Simplify this hack
          (eval `(info ~@(flatten (into [] submission))))
          (spy {:status  200
                :headers {"Content-Type" "text/plain"}
                :body    "Thank you for confirmation. Your form is delivered."}))
        (spy {:status  404
              :headers {"Content-Type" "text/plain"}
              :body    "Submission not found"})))
    (spy {:status  422
          :headers {"Content-Type" "text/plain"}
          :body    "Invalid submission uuid"})))

(defn form-handler
  [{:keys [form-params path-params ::raw-body]}]
  (let [email       (:email form-params)
        receiver-id (:receiver-id  path-params)
        receiver    (get-in @configuration [:receivers receiver-id])]
    (if (nil? receiver)
      (spy {:status  404
            :headers {"Content-Type" "text/plain"}
            :body    "No such receiver"})
      (if-not (string/blank? email)
        (let [submission-uuid  (random-uuid)
              confirmation-url (str (:base-url @configuration) "/confirm-submission/" submission-uuid)]
          (debug :prose "valid form submitted" :by email)
          (swap! submissions assoc submission-uuid
                 (assoc form-params
                        :receiver receiver
                        :raw-body raw-body))
          (send-mail nil
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

(def raw-body-interceptor
  (interceptor/interceptor
    {:name ::raw-body-interceptor
     :enter (fn [context]
              (let [body-stream (get-in context [:request :body])
                    body-string (when body-stream
                                  (slurp body-stream))]
                 (-> context
                   (assoc-in [:request ::raw-body] body-string)
                   (assoc-in [:request :body] (when body-string
                                               (java.io.ByteArrayInputStream. (.getBytes body-string)))))))
     :leave (fn [context]
                (update context :request dissoc ::raw-body))}))

(defn create-connector
  []
  (let [port    (or (:listen-port @configuration)
                    4242)
        address (or (:listen-address @configuration)
                    "0.0.0.0")]
   (-> (conn/default-connector-map address port)
       ;; our interceptor
       (conn/with-interceptor raw-body-interceptor)
       (conn/with-default-interceptors)
       (conn/with-routes routes)
       (log-connector)
       (hk/create-connector nil))))

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
