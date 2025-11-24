(ns interpreters.web-automation
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.string :refer [split-lines]]
   [etaoin.api :as e]
   [io.pedestal.log :refer [debug info warn]]
   [tbb.core :as tbb])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(def driver (e/firefox)) ;; a Firefox window should appear

;; The future is here
(def miniserve-process (atom nil))
(def form-to-mail-process (atom nil))

(def confirmation-url (atom nil))

(def server-log-file (Files/createTempFile "form-to-mail" ".log" (into-array FileAttribute [])))

(tbb/implement-step "Run the app"
                    (fn []
                      (debug "server-log-file" server-log-file)
                      (let [command (str "bb run:app 2> " server-log-file)]
                       (reset! form-to-mail-process
                              (future (sh "sh" "-c" command)))
                       ;; TODO: Be smarter about waiting. Use logs.
                       (Thread/sleep 5000))))


(tbb/implement-step "Serve {0} on port {1}"
                    (fn [path port]
                      (debug :serving path :port port)
                      (reset! miniserve-process
                              (future (sh "miniserve" "--port" "1234" "spec/samples")) )))

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
       (#(e/get-element-attr driver [{:tag :input :id %}] "type"))
       (#(tbb/tis = field-type %)))))

(tbb/implement-step
 "There is a field {0} of element {1}"
 (fn [field-label tag-name]
   (-> (e/get-element-attr driver [{:tag :label :fn/text field-label}] "for")
       (#(e/get-element-tag driver [{:tag :textarea :id %}]))
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

(defn map-includes? [small big]
 (->> small
    keys
    (select-keys big)
    (= small)))

(defn- find-matching [small coll]
  (first (filter #(map-includes? small %) coll)))

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

(tbb/implement-step
 "From a log line matching {0} extract {1}"
 (fn [query extract-key]
   (let [query       (read-string query)
         extract-key (read-string extract-key)
         found       (find-matching query (get-form-to-mail-logs))
         value       (extract-key found)]
     ;; TODO: Do a helpful assertion that something was found
     ;; TODO: Rename confirmation-url to something generic
     (reset! confirmation-url value)
     (debug :looking-for query :extracting extract-key :found found :value value))))

(tbb/implement-step
 "Open the confirmation link in the browser."
 (fn []
   (e/go driver @confirmation-url)))

(tbb/implement-step
 "There is a message {0}"
 (fn [message]
   (tbb/tis = message (e/get-element-text driver {:tag :body}))))




(defn get-current-namespace []
  (-> #'get-current-namespace meta :ns str))

(defn -main []
  (debug :prose "starting web automation")
  (tbb/ready)
  (e/close-window driver)
  (future-cancel @miniserve-process)
  (future-cancel @form-to-mail-process)
  (shutdown-agents)
  (info :done (get-current-namespace)))
