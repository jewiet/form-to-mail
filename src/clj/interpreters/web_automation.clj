(ns interpreters.web-automation
  (:require
   [clojure.java.shell :refer [sh]]
   [etaoin.api :as e]
   [io.pedestal.log :refer [debug info warn]]
   [tbb.core :as tbb]))

(def driver (e/firefox)) ;; a Firefox window should appear

;; The future is here
(def miniserve-process (atom nil))
(def form-to-mail-process (atom nil))

(tbb/implement-step "Run the app"
                    (fn []
                      (reset! form-to-mail-process
                              (future (sh "bb" "run:app" "debug")))
                      (Thread/sleep 5000)))


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

(tbb/implement-step
 "Form to Mail service will log {0}"
 (fn [text]
   (warn :todo "Currently the interpreter can't assert content of server logs."
         :variant  "Form to Mail service will log {0}"
         :asserting text)))


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
