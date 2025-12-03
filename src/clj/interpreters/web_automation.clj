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

(def server-log-file (fs/create-temp-file {:prefix "form-to-mail" :suffix ".log"}))

(def form-to-mail-process (atom nil))

(def miniserve-process (atom nil))


;; Make a smart-spy that takes the level
(defn info-spy [prose value]
  (info :prose prose :value value)
  value)

(defn debug-spy [prose value]
  (debug :prose prose :value value)
  value)


(tbb/implement-step "Run the app with the following configuration"
                    (fn [{:keys [code_blocks]}]
                      (debug "server-log-file" server-log-file)
                      (let [config (read-string (:value (first code_blocks)))
                            config-file (str (fs/create-temp-file {:prefix "form-to-mail-config"
                                                                   :suffix ".edn"}))]
                        (spit config-file config)
                        (reset! form-to-mail-process
                                (process {:err :write :err-file server-log-file} "bb run:app" config-file))
                        ;; TODO: Be smarter about waiting. Use logs.
                        (Thread/sleep 5000))))


(tbb/implement-step "Serve {0} on port {1}"
                    (fn [path port _]
                      (debug :serving path :port port)
                      (reset! miniserve-process
                              (process {:err :write :err-file server-log-file} "bb serve:samples"))
                      (Thread/sleep 1000)))

(tbb/implement-step "Navigate to {0}"
                    (fn [url _]
                      (e/go driver url)
                      (e/wait-visible driver [{:tag :body}])))
(tbb/implement-step
 "Type {0} in the {1} field"
 (fn [user-input field-label _]
   (-> (e/get-element-attr driver [{:tag :label :fn/text field-label}] "for")
       (#(e/fill driver [{:id %}] user-input)))))

(tbb/implement-step
 "Click {0} button"
 (fn [button-label _]
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

(defn table->maps [table]
  (let [[header & rows] table
        keywords        (map keyword header)]
    (map #(zipmap keywords %) rows)))

(defn table->map
  "Takes a sequence of maps with :name and :value pairings and returns a single map"
  [maps key-kw value-kw]
  (let [coll   (table->maps maps)
        ks     (map #(keyword (key-kw %)) coll)
        values (map value-kw coll)]
    (zipmap ks values)))


(tbb/implement-step
 "Form to Mail service will log {0}"
 (fn [str-entry _]
   (let [expected (read-string str-entry)]
     (->> (get-form-to-mail-logs)
          (tbb/tis has-matching? expected)))))

(tbb/implement-step
 "There is a message {0}"
 (fn [message _]
   (tbb/tis = message (e/get-element-text driver {:tag :body}))))

(tbb/implement-step
 "Click {0} radio button"
 (fn [field-label _]
   (e/click driver [{:tag :label :fn/text field-label}])))

(tbb/implement-step
 "Open the inbox of {0}"
 (fn [email-address _]
   (->> (get-form-to-mail-logs)
        (debug-spy "All logs")
        (filter-matching {:prose "sending an email" :to email-address})
        (map #(dissoc % :prose))
        (debug-spy "Inbox from logs")
        (reset! current-inbox))))

(tbb/implement-step
 "In the inbox find the message with the subject {0}"
 (fn [subject _]
   (->> @current-inbox
        (debug-spy "Current inbox")
        (find-matching {:subject subject})
        (debug-spy "Found message")
        (reset! current-message)
        (tbb/tis some? ))))

(tbb/implement-step
 "In the message open the link labeled {0}"
 (fn [label _]
   (let [pattern (re-pattern (str "<a\\s+.*href\\s*=\\s*'(.+)'.*>" label "</a>"))]
     (->> @current-message
          :body
          (debug-spy "Body")
          (re-find pattern)
          (last)
          (debug-spy "URL to open")
          (e/go driver)))))

(tbb/implement-step
 "There is a {0} element with the following properties"
 (fn [element-type {:keys [tables]}]
   (let [first-table (first tables)
         attributes (table->map first-table :name :value)]
     (e/get-element-tag driver (assoc attributes :tag element-type)))))

(defn- field-row->query [id field-row]
  (-> field-row
      (assoc :id id)
      (dissoc :label)
      (#(case (:type %)
          ("textarea" "select") (dissoc (assoc % :tag (:type %) ) :type)
          %))))


(tbb/implement-step
 "There are the following fields"
 (fn [{:keys [tables]}]
   (let [field-rows (-> tables
                        (first)
                        (table->maps))]
     (doseq [field-row field-rows]
       (-> (e/get-element-attr driver [{:tag :label :fn/text (:label field-row)}] "for")
           (field-row->query field-row)
           (#(e/get-element-tag driver %))
           (#(debug-spy "found-field" %)))))))

(tbb/implement-step
 "Enter the following input in to the corresponding fields"
 (fn [{:keys [tables]}]
   (let [field-rows (-> tables
                        (first)
                        (table->maps))]
     (doseq [field-row field-rows]
       (-> (e/get-element-attr driver {:tag :label :fn/text (:label field-row)} "for")
           (field-row->query field-row)
           (#(e/fill driver {:id (:id %)} (:user-input %))))))))

(tbb/implement-step
 "The message has reply-to header {0}"
 (fn [relpy-to _]
   (tbb/tis = (:reply-to @current-message) relpy-to)))

(tbb/implement-step
 "The message contains a clojure map with the following fields"
 (fn [{:keys [tables]}]
   (let [expected (table->map (first tables) :key :value)
         actual   (read-string (:body @current-message))]
     (tbb/tis map-includes? expected actual))))


(defn get-current-namespace []
  (-> #'get-current-namespace meta :ns str))

(defn -main []
  (debug :prose "starting web automation")
  (tbb/ready)
  (e/quit driver)
  (when @form-to-mail-process
   (destroy-tree @form-to-mail-process))
  (when @miniserve-process
   (destroy-tree @miniserve-process))
  (info :done (get-current-namespace)))
