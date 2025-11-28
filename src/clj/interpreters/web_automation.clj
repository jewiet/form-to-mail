(ns interpreters.web-automation
  (:require
   [clojure.string :refer [split-lines]]
   [etaoin.api :as e]
   [io.pedestal.log :refer [debug info warn]]
   [babashka.fs :as fs]
   [babashka.process :as p :refer [process destroy-tree]]
   [tbb.core :as tbb]))

(def current-inbox (atom nil))

(def current-message (atom nil))

(def driver (e/firefox)) ;; a Firefox window should appear

;; TODO: Remove it in favor of reading information from inboxes
(def confirmation-url (atom nil))

(def server-log-file (fs/create-temp-file {:prefix "form-to-mail" :suffix ".log"}))

(def form-to-mail-process (process {:err :write :err-file server-log-file} "bb run:app"))

(def miniserve-process (process {:err :write :err-file server-log-file} "bb serve:samples"))

(tbb/implement-step "Run the app"
                    (fn []
                      (debug "server-log-file" server-log-file)
                      form-to-mail-process
                      ;; TODO: Be smarter about waiting. Use logs.
                      (Thread/sleep 5000)))


(tbb/implement-step "Serve {0} on port {1}"
                    (fn [path port]
                      (debug :serving path :port port)
                      miniserve-process))

(tbb/implement-step "Navigate to {0}"
                    (fn [url]
                      (e/go driver url)
                      (e/wait-visible driver [{:tag :body}])))
(tbb/implement-step
 "the form {0} is set to {1}"
 (fn [attribute-name attribute-value]
   (tbb/tis = (e/get-element-attr driver [{:tag :form}] attribute-name)
            attribute-value)))

(tbb/implement-step
 "There is a field {0} of type {1}"
 (fn [field-label field-type]
   (-> (e/get-element-attr driver [{:tag :label :fn/text field-label}] "for")
       (#(e/get-element-attr driver [{:id %}] "type"))
       (#(tbb/tis = field-type %)))))

(tbb/implement-step
 "There is a field {0} of element {1}"
 (fn [field-label tag-name]
   (-> (e/get-element-attr driver [{:tag :label :fn/text field-label}] "for")
       (#(e/get-element-tag driver [{:id %}]))
       (#(tbb/tis = tag-name %)))))

(tbb/implement-step
 "Type {0} in the {1} field"
 (fn [user-input field-label]
   (-> (e/get-element-attr driver [{:tag :label :fn/text field-label}] "for")
       (#(e/fill driver [{:id %}] user-input)))))

(tbb/implement-step
 "Click {0} button"
 (fn [button-label]
   (e/click driver [{:tag :button :fn/text button-label}])))

;; TODO: Move to a different namespace or to the top
(defn get-form-to-mail-logs []
  (->> server-log-file
       str
       slurp
       split-lines
       (keep #(re-find #"\{.*\}" %))
       (map read-string)
       doall))

;; Make a smart-spy that takes the level
(defn info-spy [prose value]
  (info :prose prose :value value)
  value)

(defn map-includes? [small big]
  (->> small
       keys
       (select-keys big)
       (= small)))

(defn- filter-matching [small coll]
  (filter #(map-includes? small %) coll))

(defn- find-matching [small coll]
  (->> coll
       (filter-matching small)
       (first)))

(defn has-matching?
  "Takes a small map and a collection of maps, checks if any matches"
  [small coll]
  (not-empty (find-matching small coll)))

(tbb/implement-step
 "Form to Mail service will log {0}"
 (fn [str-entry]
   (let [expected (read-string str-entry)]
     (->> (get-form-to-mail-logs)
          (tbb/tis has-matching? expected)))))

;; TODO: Remove it in favor of reading information from inboxes
(tbb/implement-step
 "From a log line matching {0} extract {1}"
 (fn [query extract-key]
   (let [query       (read-string query)
         extract-key (read-string extract-key)
         found       (find-matching query (get-form-to-mail-logs))
         value       (extract-key found)]
     ;; TODO: Do a helpful assertion that something was found
     ;; TODO: Rename confirmation-url to something generic
     (reset! confirmation-url value))))

;; TODO: Remove it in favor of reading information from inboxes
(tbb/implement-step
 "Open the confirmation link in the browser."
 (fn []
   (e/go driver @confirmation-url)))

(tbb/implement-step
 "There is a message {0}"
 (fn [message]
   (tbb/tis = message (e/get-element-text driver {:tag :body}))))

(tbb/implement-step
 "There is a radio button labeled {0}"
 (fn [field-label]
   (-> (e/get-element-attr driver [{:tag :label :fn/text field-label}] "for")
       (#(e/get-element-attr driver [{:tag :input :id %}] "type"))
       (#(tbb/tis = "radio" %)))))

(tbb/implement-step
 "Click {0} radio button"
 (fn [field-label]
   (e/click driver [{:tag :label :fn/text field-label}])))

(tbb/implement-step
 "Select {0} in the {1} field"
 (fn [user-input field-label]
   (-> (e/get-element-attr driver [{:tag :label :fn/text field-label}] "for")
       (#(e/fill driver [{:id %}] user-input)))))

(tbb/implement-step
 "Open the inbox of {0}"
 (fn [email-address]
   (->> (get-form-to-mail-logs)
        (info-spy "All logs")
        (filter-matching { :prose "sending an email" :to email-address })
        (map #(dissoc % :prose))
        (info-spy "Inbox from logs")
        (reset! current-inbox))))

(tbb/implement-step
 "In the inbox find the message with the subject {0}"
 (fn [subject]
   (->> @current-inbox
        (info-spy "Current inbox")
        (find-matching {:subject subject})
        (info-spy "Found message")
        (reset! current-message))))

(tbb/implement-step
 "In the message open the link labeled {0}"
 (fn [label]
   (let [pattern (re-pattern (str "<a\\s+.*href\\s*=\\s*'(.+)'.*>" label "</a>"))]
     (->> @current-message
         :body
         (info-spy "Body")
         (re-find pattern)
         (last)
         (info-spy "URL to open")
         (e/go driver)))))

(defn get-current-namespace []
  (-> #'get-current-namespace meta :ns str))

(defn -main []
  (debug :prose "starting web automation")
  (tbb/ready)
  (e/close-window driver)
  (destroy-tree form-to-mail-process)
  (destroy-tree miniserve-process)
  (info :done (get-current-namespace)))
