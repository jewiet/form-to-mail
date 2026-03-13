(ns interpreters.http
  (:require
   [babashka.fs :as fs]
   [babashka.process :refer [destroy-tree process]]
   [clojure.string :refer [lower-case]]
   [io.pedestal.log :refer [debug spy]]
   [org.httpkit.client :as hk-client]
   [tbb.core :as tbb]))

(defonce response (atom nil))

(def server-log-file (fs/create-temp-file {:prefix "form-to-mail" :suffix ".log"}))

(def form-to-mail-process (atom nil))

;; TODO: Implement "Run the app" with a default configuration to reduce spec boilerplate
;; TODO: Extract logic shared with web-automation interpreter to a shared module

(tbb/implement-step
 "Run the app with the following configuration"
 (fn [{:keys [code_blocks]}]
   (debug "server-log-file" server-log-file)
   (let [config (read-string (:value (first code_blocks)))
         config-file (str (fs/create-temp-file {:prefix "form-to-mail-config"
                                                :suffix ".edn"}))]
     (spit config-file config)
     (reset! form-to-mail-process
             (process {:err :write
                       :err-file server-log-file}
                      "bb app:run" config-file))
                        ;; TODO: Be smarter about waiting. Use logs.
     (Thread/sleep 5000))))

(tbb/implement-step
 "Make a {0} request to {1}."
 (fn [method url _]
   (debug :prose "making http request" :method method :url url)
   (let [method (keyword (lower-case method))]
     (-> {:method method
          :url url}
         hk-client/request
         deref
         (#(reset! response %))
         (spy)))))

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

(defn -main []
  (debug :prose "Starting http interpreter")
  (tbb/ready)
  (debug :prose "Stopping the server.")
  (when @form-to-mail-process
    (destroy-tree @form-to-mail-process)))
