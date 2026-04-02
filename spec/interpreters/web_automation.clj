(ns web-automation
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p :refer [destroy-tree process]]
   [clojure.string :as string]
   [clojure.walk :as walk]
   [common :refer [filter-matching find-matching has-matching? map-includes?
                   read-log-file wait-for-log]]
   [etaoin.api :as e]
   [ring.util.codec :refer [base64-decode form-decode]]
   [taoensso.timbre :as logging]
   [tbb]))

(def current-inbox (atom nil))

(def current-message (atom nil))

(def driver (atom nil))

(def server-log-file (fs/create-temp-file {:prefix "form-to-mail" :suffix ".log"}))

(def form-to-mail-process (atom nil))

(def miniserve-process (atom nil))

(tbb/implement-step
 "Run the app with the following configuration"
 (fn [{:keys [code_blocks]}]
   (logging/debug "server-log-file" server-log-file)
   (let [config (read-string (:value (first code_blocks)))
         config-file (str (fs/create-temp-file {:prefix "form-to-mail-config"
                                                :suffix ".edn"}))]
     (spit config-file config)
     (reset! form-to-mail-process
             (process {:err :write
                       :err-file server-log-file}
                      "bb app:run" config-file))
     (wait-for-log {:prose "Starting Form to Mail"}
                   server-log-file))))

(tbb/implement-step
 "Serve {0} on port {1}"
 (fn [path port _]
   (logging/debug "Serving" {:path path
                             :port port})
   (reset! miniserve-process
           (process {:err :write :err-file server-log-file}
                    "miniserve --port" port path))
   (Thread/sleep 1000)))

(tbb/implement-step
 "Navigate to {0}"
 (fn [url _]
   (e/go @driver url)
   (e/wait-visible @driver [{:tag :body}])))

(tbb/implement-step
 "Type {0} in the {1} field"
 (fn [user-input field-label _]
   (-> (e/get-element-attr @driver [{:tag :label :fn/text field-label}] "for")
       (#(e/fill @driver [{:id %}] user-input)))))

(tbb/implement-step
 "Click {0} button"
 (fn [button-label _]
   (e/click @driver [{:tag :button :fn/text button-label}])))

;; TODO: Move to a different namespace or to the top

(tbb/implement-step
 "Form to Mail service will log {0}"
 (fn [str-entry _]
   (let [expected (read-string str-entry)]
     (->> (read-log-file server-log-file)
          (tbb/tis has-matching? expected)))))

(tbb/implement-step
 "There is a message {0}"
 (fn [message _]
   (tbb/tis = message (e/get-element-text @driver {:tag :body}))))

(tbb/implement-step
 "The webpage contains {0}"
 (fn [message _]
   (string/includes? (e/get-element-text @driver {:tag :body}) message)))

(tbb/implement-step
 "Click {0} radio button"
 (fn [field-label _]
   (e/click @driver [{:tag :label :fn/text field-label}])))

(tbb/implement-step
 "Open the inbox of {0}"
 (fn [email-address _]
   (tbb/send-link server-log-file, "Server logs")
   (->> (read-log-file server-log-file)
        (filter-matching {:prose "sending an email"})
        (tbb/send-text)
        (filter #(some #{email-address} (:to %)))
        (map #(dissoc % :prose))
        (tbb/send-text)

        (reset! current-inbox))
   (tbb/send-text @current-inbox)))

(tbb/implement-step
 "In the inbox find the message with the subject {0}"
 (fn [subject _]
   (->> @current-inbox
        (logging/spy :debug "Current inbox")
        (find-matching {:subject subject})
        (logging/spy :debug "Found message")
        (reset! current-message)
        (tbb/tis some?))))

(tbb/implement-step
 "In the message open the link labeled {0}"
 (fn [label _]
   (let [pattern (re-pattern (str "<a\\s+.*href\\s*=\\s*[\"'](.+)[\"'].*>" label "</a>"))]
     (->> @current-message
          :body
          first
          :content
          (re-find pattern)
          (logging/spy :debug "link")
          (last)
          (logging/spy :debug "URL to open")
          (e/go @driver)))))

(tbb/implement-step
 "There is a {0} element with the following properties"
 (fn [element-type {:keys [tables]}]
   (let [first-table (first tables)
         attributes (tbb/table->map first-table :name :value)]
     (e/get-element-tag @driver (assoc attributes :tag element-type)))))

(defn- field-row->query [id field-row]
  (-> field-row
      (assoc :id id)
      (dissoc :label)
      (#(case (:type %)
          ("textarea" "select") (dissoc (assoc % :tag (:type %)) :type)
          %))))

(tbb/implement-step
 "There are the following fields"
 (fn [{:keys [tables]}]
   (let [field-rows (-> tables
                        (first)
                        (tbb/table->maps))]
     (doseq [field-row field-rows]
       (-> (e/get-element-attr @driver [{:tag :label :fn/text (:label field-row)}] "for")
           (field-row->query field-row)
           (#(e/get-element-tag @driver %))
           (#(logging/spy :debug "found-field" %)))))))

(tbb/implement-step
 "Enter the following input in to the corresponding fields"
 (fn [{:keys [tables]}]
   (let [field-rows (-> tables
                        (first)
                        (tbb/table->maps))]
     (doseq [field-row field-rows]
       (-> (e/get-element-attr @driver {:tag :label :fn/text (:label field-row)} "for")
           (field-row->query field-row)
           (#(e/fill @driver {:id (:id %)} (:user-input %))))))))

(tbb/implement-step
 "The message has reply-to header {0}"
 (fn [relpy-to _]
   (tbb/tis = (:reply-to @current-message) relpy-to)))

(tbb/implement-step
 "The message contains an application/x-www-form-urlencoded encoded attachment with the following fields"
 (fn [{:keys [tables]}]
   (let [expected (tbb/table->map (first tables) :key :value)
         actual   (->> @current-message
                       :body
                       (find-matching {:file-name "form-to-mail-request-body.txt"})
                       :content
                       base64-decode
                       String.
                       form-decode
                       walk/keywordize-keys)]

     (tbb/tis map-includes? expected actual))))

(tbb/implement-step
 "The {0} body of the message contains"
 (fn [type {:keys [code_blocks]}]
   (let [expected (->>  code_blocks
                        first
                        :value
                        string/trim)
         actual   (->> @current-message
                       :body
                       (find-matching {:type type})
                       :content
                       string/trim)]

     (tbb/tis string/includes? actual expected))))

(defn -main [& args]
  (logging/info "Interpreter start")
  (reset! driver (e/firefox))
  (tbb/ready)
  (e/quit @driver)
  (when @form-to-mail-process
    (destroy-tree @form-to-mail-process))
  (when @miniserve-process
    (destroy-tree @miniserve-process))
  (logging/info "Interpreter done"))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
