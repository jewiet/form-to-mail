(ns http
  (:require
   [babashka.fs :as fs]
   [babashka.process :refer [destroy-tree process]]
   [clojure.string :refer [lower-case]]
   [common :refer [wait-for-log]]
   [org.httpkit.client :as hk-client]
   [taoensso.timbre :as logging]
   [tbb]))

(defonce response (atom nil))

(def server-log-file (fs/create-temp-file {:prefix "form-to-mail" :suffix ".log"}))

(def form-to-mail-process (atom nil))

;; TODO: Implement "Run the app" with a default configuration to reduce spec boilerplate
;; TODO: Extract logic shared with web-automation interpreter to a shared module

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
 "Make a {0} request to {1}."
 (fn [method url _]
   (logging/debug "making http request" {:method method :url url})
   (let [method (keyword (lower-case method))]
     (->> {:method method
           :url url}
          hk-client/request
          deref
          (reset! response)
          (logging/spy :debug)))))

(tbb/implement-step
 "The response has a {0} status code."
 (fn [status _]
   (let [actual (:status @response)
         expected (read-string status)]
     (tbb/tis = expected actual))))

(tbb/implement-step
 "The response body is {0}."
 (fn [body _]
   (tbb/tis = body (:body @response))))

(tbb/implement-step
 "The response {0} header is {1}."
 (fn [header-name header-value _]
   (let [header-key (keyword header-name)]
     (tbb/tis = header-value (get-in @response [:headers header-key])))))

(defn -main [& args]
  (logging/info "Interpreter start")
  (tbb/ready)
  (logging/debug "Stopping the server.")
  (when @form-to-mail-process
    (destroy-tree @form-to-mail-process))
  (logging/info "Interpreter done"))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
