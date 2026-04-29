(ns app.server
  (:require
   [app.templates :as templates]
   [clojure.string :as string]
   [hiccup2.core :as h]
   [io.pedestal.connector :as conn]
   [io.pedestal.environment :refer [dev-mode?]]
   [io.pedestal.http.http-kit :as hk]
   [io.pedestal.interceptor :as interceptor]
   [io.pedestal.log :refer [debug info spy]]
   [io.pedestal.service.resources :as resources]
   [postal.core :as postal]
   [ring.util.codec :refer [base64-encode]]))

(defonce configuration (atom nil))

(defonce submissions (atom {}))

(defn- encode-body-part [part]
  (if (= (:type part) :attachment)
    (update part :content base64-encode)
    part))

(defn- encode-body-parts [body]
  (map encode-body-part body))

(defn send-mail
  "Send an email.

  If config has :smpt-server key, use its value. Otherwise log details of the
  message for mocking.

  The first argument (reply-to) can be nil if the message shouldn't be replied
  to."

  [reply-to to subject body]
  ;; Normalize to address to always be a vector
  (let [to (if (vector? to) to [to])]
    (if-let [smtp-config (:smtp-server @configuration)]
      (postal/send-message smtp-config
                           {:from     (:from-address @configuration)
                            :reply-to reply-to
                            :to       to
                            :subject  subject
                            :body     body})
      ;; TODO: DRY
      (info :prose "sending an email"
            :from (:from-address @configuration)
            :to to
            :reply-to reply-to
            :subject subject
            :body (encode-body-parts body)))))

(defn submission-verification [{:keys [path-params]}]
  (debug :prose "verifying submission" :submissions @submissions)
  (if-let [submission-uuid (parse-uuid (:submission-uuid path-params))]
    (do
      (debug :prose "verifying submission id"  :submission-uuid submission-uuid)
      (if-let [{:keys [raw parsed receiver] :as submission} (get @submissions submission-uuid)]
        (do
          (debug :prose "found submission" :submission submission)
          (send-mail (:sender parsed)
                     (:email-addresses receiver)
                     "Form to Mail message"
                     ;; Use a templating library
                     (templates/delivery-email-html parsed raw))
          (when-not dev-mode?
            ;; In production prevent the submission from being delivered multiple times
            ;; In development keep it, so we can iterate quickly
            (swap! submissions dissoc submission-uuid))

          ;; TODO: Simplify this hack
          (eval `(info ~@(flatten (into [] submission))))
          (spy {:status  200
                :headers {"Content-Type" "text/html",
                          "Refresh" (str "10, url=" (:return-url receiver))}
                :body    (templates/confirmation (select-keys receiver [:receiver-name :return-url]))}))
        (spy {:status  404
              :headers {"Content-Type" "text/plain"}
              :body    "Submission not found"})))
    (spy {:status  422
          :headers {"Content-Type" "text/plain"}
          :body    "Invalid submission uuid"})))

(defn form-handler
  [{:keys [form-params path-params ::raw-body]}]
  (let [sender         (:sender form-params)
        receiver-id    (:receiver-id  path-params)
        receiver       (get-in @configuration [:receivers receiver-id])]
    (if (nil? receiver)
      (spy {:status  404
            :headers {"Content-Type" "text/plain"}
            :body    "No such receiver"})
      (if-not (string/blank? sender)
        (let [submission-uuid  (random-uuid)
              confirmation-url (str (:base-url @configuration) "/confirm-submission/" submission-uuid)]
          (info :prose "valid form submitted" :by sender)
          (swap! submissions assoc submission-uuid
                 (-> {}
                     (assoc :parsed form-params)
                     (assoc :receiver receiver)
                     (assoc :raw raw-body)))
          (send-mail nil
                     sender
                     "Form to Mail confirmation"
                     (templates/confirmation-email-html confirmation-url))
          (spy {:status  200
                :headers {"Content-Type" "text/html"}
                :body    (templates/submission sender)}))
        (do
          (info :prose "Missing required field" :field "sender")
          (spy {:status  422
                :headers {"Content-Type" "text/plain"}
                :body    "Missing required field sender"}))))))

(def routes
  #{["/submit/:receiver-id"
     :post form-handler
     :route-name :form-submit]
    ["/confirm-submission/:submission-uuid"
     :get submission-verification
     :route-name :submission-verification]})

(defn log-connector [connector-map]
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
        (conn/with-routes routes
          (resources/resource-routes {:prefix "/"
                                      :resource-root "public"}))
        (log-connector)
        (hk/create-connector nil))))

;; For interactive development
(defonce *connector (atom nil))

(defn start [config]
  (reset! configuration config)
  (reset! *connector
          (conn/start! (create-connector)))
  (info :prose "Starting Form to Mail" :config (select-keys config [:listen-port :listen-address])))

(defn stop []
  (conn/stop! @*connector)
  (reset! *connector nil))

(defn restart []
  (stop)
  (start @configuration))
