(ns app.server
  (:require
   [clojure.string :as string]
   [io.pedestal.connector :as conn]
   [io.pedestal.http.http-kit :as hk]
   [io.pedestal.interceptor :as interceptor]
   [io.pedestal.log :refer [debug info spy]]
   [postal.core :as postal]
   [hiccup2.core :as h]
   [clojure.java.io :as io]
   [hiccup.page :refer [html5]]
   [ring.util.codec :refer [base64-encode]]))

(defn- value->html [v]
  (if (vector? v)
    (map value->html v)
    [:dd v]))

(defn form->html [form]
  (str (h/html [:html
                [:head]
                [:body
                 [:p "Contents of the form submitted"]
                 [:dl (map (fn [[k v]]
                             (list [:dt (name k)]
                                   (value->html v)))
                           form)]]])))

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
          :body (encode-body-parts body))))

(defn submission-verification [{:keys [path-params]}]
  (debug :prose "verifying submission" :submissions @submissions)
  (if-let [submission-uuid (parse-uuid (:submission-uuid path-params))]
    (do
      (debug :prose "verifying submission id"  :submission-uuid submission-uuid)
      (if-let [{:keys [raw parsed receiver] :as submission} (get @submissions submission-uuid)]
        (do
          (debug :prose "found submission" :submission submission)
          (send-mail (:email parsed)
                     receiver
                     "Form to Mail message"
                     ;; Use a templating library
                     [{:type "text/html"
                       :content (form->html parsed)}
                      {:type :attachment
                       :file-name "form-to-mail-request-body.txt"
                       :content-type "application/x-www-form-urlencoded"
                       :content (.getBytes raw)}])
          (swap! submissions dissoc submission-uuid)

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
          (info :prose "valid form submitted" :by email)
          (swap! submissions assoc submission-uuid
                 (-> {}
                     (assoc :parsed form-params)
                     (assoc :receiver receiver)
                     (assoc :raw raw-body)))
          (send-mail nil
                     email
                     "Form to Mail confirmation"
                     [{:type "text/html"
                       :content (str (h/html [:html
                                              [:head
                                               [:style ".call-to-action {
                                                            background: darkblue;
                                                            padding: 0.5em 1em;
                                                            border-radius: 1em;
                                                            color: white;
                                                            font-weight: bold;
                                                            text-decoration: none;
                                                        }"]]
                                              [:body
                                               [:p
                                                "Thank you for submitting the form at "
                                                [:a {:href "https://formtomail.eu/"} "Form to Mail"]
                                                ". Before we deliver your form we need to confirm your email address. Please click below."]
                                               [:p
                                                [:a {:href confirmation-url :class "call-to-action"} "Confirm your submission"]
                                                [:p "If you haven't filled the form please ignore this email."]]]]))}])
          (spy {:status  200
                :headers {"Content-Type" "text/plain"}
                :body    (str "Thank you for sending the form. We have sent you an email with confirmation link to " email)}))
        (do
          (info :prose "Missing required field" :field "email")
          (spy {:status  422
                :headers {"Content-Type" "text/plain"}
                :body    "Missing required field email"}))))))

(defn resource-handler
  [request]
  (if-let [resource-url (io/resource (str "public/" (get-in request [:path-params :resource])))]
    (spy {:status  200
          :headers (if (clojure.string/ends-with? resource-url ".css")
                     {"Content-Type" "text/css"}
                     {"Content-Type" "image/svg+xml"})
          :body  (slurp resource-url)})
    (spy {:status  404
          :body  "Resource not found"})))

(defn home-handler
  [_request]
  (let [html-head              [:head
                                [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                                [:link {:type "text/css", :href "/resources/pico.min.css", :rel "stylesheet"}]
                                [:link {:type "text/css", :href "/resources/style.css", :rel "stylesheet"}]]
        html-header            [:header.container
                                [:hgroup
                                 [:h1 "Form to Mail"]
                                 [:p "Web forms for static websites"]]]
        intro-section          [:section#intro
                                [:p "Form to Mail is a lightweight web service that sends data submitted from a standard HTML form as an email via any SMTP server. The service verifies sender's email address before delivering the form. It works with standard HTML forms and it's easy to self host."]]
        features-section       [:section#features.grid
                                [:div
                                 [:img.icon {:src "/resources/streamline-emojis--wrench.svg"}]
                                 [:p
                                  "Standard HTML forms"
                                  [:small "without JavaScript or other bullshit"]]]
                                [:div
                                 [:img.icon {:src "/resources/streamline-emojis--open-mailbox-with-raised-flag.svg"}]
                                 [:p
                                  "Verifies sender's address"
                                  [:small "before delivering you a form"]]]
                                [:div
                                 [:img.icon {:src "/resources/streamline-emojis--house-with-garden.svg"}]
                                 [:p
                                  "Delightful to self-host"
                                  [:small "with " [:code "java -jar"] " or a NixOS module"]]]]
        call-to-action-section [:section#call-to-action.grid
                                [:a {:href   "https://github.com/jewiet/form-to-mail/releases"
                                     :target "_blank"
                                     :role   "button"}
                                 "Download"]
                                [:a {:href   "https://github.com/jewiet/form-to-mail/"
                                     :target "_blank"
                                     :role   "button"
                                     :class  "outline"}
                                 "More info"]]
        contact-section        [:section#contact
                                [:h3 "Get in touch"]
                                [:form {:method "POST" :action "https://formtomail.eu/submit/1234"}
                                 [:label {:for "email"} "Your email"]
                                 [:input {:type "email" :name "email" :id "email" :placeholder "Enter your email address" :required true}]
                                 [:label {:for "message"} "Your message"]
                                 [:textarea {:name "message" :id "message" :placeholder "If you write a nice message, I will reply back."}]
                                 [:button {:type "submit"} "Send"]]]
        html-main              [:main {:class "container"}
                                intro-section
                                features-section
                                call-to-action-section
                                contact-section]
        html-body              [:body
                                html-header
                                html-main]
        body                   (str (html5
                                     html-head
                                     html-body))]
    (spy {:status  200
          :headers {"Content-Type" "text/html"}
          :body    body})))

(def routes
  #{["/"
     :get home-handler
     :route-name :home]
    ["/submit/:receiver-id"
     :post form-handler
     :route-name :form-submit]
    ["/confirm-submission/:submission-uuid"
     :get submission-verification
     :route-name :submission-verification]
    ["/resources/:resource" :get resource-handler :route-name :resource]})

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
