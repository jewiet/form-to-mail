(ns web-automation
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p :refer [destroy-tree process shell]]
   [clojure.string :as string]
   [common :refer [has-matching? read-log-file wait-for-log]]
   [etaoin.api :as e]
   [etaoin.keys :as k]
   [taoensso.timbre :as logging]
   [tbb]))

(def driver (atom nil))

(def server-log-file (fs/create-temp-file {:prefix "form-to-mail" :suffix ".log"}))

(def extra-env {"FAKETIME_TIMESTAMP_FILE" (fs/create-temp-file {:prefix "form-to-mail"
                                                                :suffix "faketime"})})

(def form-to-mail-process (atom nil))

(def miniserve-process (atom nil))

(def mailpit-process (atom nil))

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
                       :err-file server-log-file
                       :extra-env extra-env}
                      "clojure -M -m app.core" config-file))
     (wait-for-log {:prose "Starting Form to Mail"}
                   server-log-file))))

(tbb/implement-step
 "Serve {0} on port {1}"
 (fn [path port _]
   (logging/debug "Serving" {:path path
                             :port port})
   (reset! miniserve-process
           (process {:err :write
                     :err-file server-log-file
                     :extra-env extra-env}
                    "miniserve --port" port path))
   (Thread/sleep 1000)))

(tbb/implement-step
 "Run Mailpit"
 (fn [_]
   (logging/debug "Starting mailpit")
   (reset! mailpit-process
           (process {:err :write
                     :err-file server-log-file
                     :extra-env extra-env}
                    "mailpit"))))

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
 (fn [text _]
   (tbb/send-text (str "Looking for `" text "` on " (e/get-url @driver)))
   (e/wait-has-text-everywhere @driver text)))

(tbb/implement-step
 "The webpage contains the following"
 (fn [{:keys [code_blocks]}]
   (let [texts (map :value code_blocks)]
     (doall (for [text texts]
              (do (tbb/send-text (str "Looking for " text))
                  (e/wait-has-text-everywhere @driver text)))))))

(tbb/implement-step
 "There is a link {0} to {1}"
 (fn [label href _] (let [found-href (e/get-element-attr @driver {:tag :a :fn/text label} "href")]
                      (tbb/send-text (str "Found link to " found-href))
                      (tbb/tis = found-href href))))

(tbb/implement-step
 "There are no messages in the inbox of {0}"
 (fn [email-address _]
   (e/fill @driver {:tag :input :placeholder "Search mailbox"}
           (str "to:" email-address) k/enter)
   (tbb/tis =
            (str "No results for to:" email-address)
            (e/get-element-property @driver {:id "message-page"} "innerText"))))

(tbb/implement-step
 "Click {0} radio button"
 (fn [field-label _]
   (e/click @driver [{:tag :label :fn/text field-label}])))

(tbb/implement-step
 "Open the inbox of {0}"
 (fn [email-address _]
   (e/go @driver "http://localhost:8025/")
   (e/fill @driver {:tag :input :placeholder "Search mailbox"} (str "to:" email-address) k/enter)))

(tbb/implement-step
 "In the inbox find the message with the subject {0}"
 (fn [subject _]
   (tbb/send-text (str "Looking for the subject `" subject "` at " e/get-url))
   (e/click @driver
            [{:fn/has-class "subject"}
             {:fn/has-text subject}])))

(tbb/implement-step
 "In the message open the link labeled {0}"
 (fn [label _]
   (let [link  (e/with-frame @driver :preview-html
                 (e/get-element-attr @driver
                                     {:tag     :a
                                      :class   :call-to-action
                                      :fn/text label}
                                     :href))]
     (e/go @driver link))))

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
   (tbb/tis =
            relpy-to
            (e/get-element-inner-html @driver [{:tag :table :class "messageHeaders"}
                                               {:tag :a :class "text-body-secondary"}]))))

(tbb/implement-step
 "The message contains an application/x-www-form-urlencoded encoded attachment with the following fields"
 (fn [_]
   (tbb/tis =
            "form-to-mail-request-body.txt"
            (e/get-element-text @driver {:tag :a :class "card attachment float-start me-3 mb-3"}))))

(tbb/implement-step
 "The {0} body of the message contains"
 (fn [_ {:keys [code_blocks]}]
   (let [expected (->>  code_blocks
                        first
                        :value
                        string/trim)]
     (tbb/send-text "expected")
     (tbb/send-text expected)
     (e/switch-frame @driver {:id :preview-html})
     (tbb/send-text "actual")
     (tbb/send-text (e/get-element-inner-html @driver {:tag :table}))
     (tbb/tis string/includes? expected (e/get-element-inner-html @driver {:tag :table}))
     (e/switch-frame-top @driver))))

(tbb/implement-step
 "Load {0} in the {1} field"
 (fn [file-path field-label _]
   (-> (e/get-element-attr @driver [{:tag :label :fn/text field-label}] "for")
       (#(e/upload-file @driver [{:tag :input :type :file :id %}] file-path)))))

(tbb/implement-step
 "Wait {0}"
 (fn [duration _data]
   (shell {:extra-env extra-env
           :out       :string}
          "date" "--set" (str "+" duration))
   (Thread/sleep 10000)))

(defn- stop-process [subject]
  (when subject
    (logging/info "Stopping the process" (:cmd subject))
    (destroy-tree subject)
    (let [zombie (:proc subject)]
      (when (.isAlive zombie)
        (logging/warn "It's still alive! Murder!" zombie)
        (->> zombie
             .toHandle
             .pid
             str
             (shell "kill" "-9"))))))

(defn -main [& args]
  (logging/info "Interpreter start")
  (reset! driver (e/firefox))
  (tbb/ready)
  (e/quit @driver)
  (stop-process @form-to-mail-process)
  (stop-process @miniserve-process)
  (stop-process @mailpit-process)
  (logging/info "Interpreter done"))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
