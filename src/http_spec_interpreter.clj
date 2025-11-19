(ns http-spec-interpreter
  (:require
   [app.server :as server]
   [clojure.data.json :as json]
   [clojure.string :refer [blank? lower-case]]
   [io.pedestal.log :refer [info spy]]
   [org.httpkit.client :as hk-client]
   [tbb.core :refer [tis]]))

(defonce response (atom nil))

(def steps-implementation
  {"Run the app"
   (fn []
     (server/start))

   "Make a {0} request to {1}."
   (fn [method url]
     (info :prose "making http request" :method method :url url)
     (let [method (keyword (lower-case method))]
       (-> {:method method
            :url url}
           hk-client/request
           deref
           (#(reset! response %))
           (spy))))

   "The response has a {0} status code."
   (fn [status]
     (let [actual (:status @response)
           expected (read-string status)]
       (tis = expected actual)))

   "The response body is {0}."
   (fn [body]
     (tis = body (:body @response)))

   "The response {0} header is {1}."
   (fn [header-name header-value]
     (let [header-key (keyword header-name)]
       (tis = header-value (get-in @response [:headers header-key]))))})

(println (json/write-str {:type "InterpreterState"
                          :ready true}))

(doseq [line (line-seq (java.io.BufferedReader. *in*))]
  (when-not (blank? line)
    (let [message       (json/read-str line :key-fn keyword)
          variant       (get-in message [:step :variant])
          arguments     (get-in message [:step :arguments])
          implmentation (get steps-implementation variant)]
      (info :prose "got a message from tbb" :message message)
      (if (nil? implmentation)
        (println (json/write-str {:type   "Failure"
                                  :reason "Not implemented"}))
        (try
          (apply implmentation arguments)
          (println (json/write-str {:type "Success"}))
          (catch AssertionError e
            (println (json/write-str {:type   "Failure"
                                      :reason (.getMessage e)}))))))))

(info :prose "Done reading from tbb. Stopping the server.")
(server/stop)
