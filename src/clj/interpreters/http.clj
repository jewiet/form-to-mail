(ns interpreters.http
  (:require
   [app.server :as server]
   [clojure.string :refer [lower-case]]
   [io.pedestal.log :refer [info spy]]
   [org.httpkit.client :as hk-client]
   [tbb.core :refer [tis implement-step ready]]))

(defonce response (atom nil))

(implement-step "Run the app"
                (fn []
                  (server/start)))

(implement-step  "Make a {0} request to {1}."
                 (fn [method url]
                   (info :prose "making http request" :method method :url url)
                   (let [method (keyword (lower-case method))]
                     (info :method method :url url)
                     (-> {:method method
                          :url url}
                         hk-client/request
                         deref
                         (#(reset! response %))
                         (spy)))))

(implement-step "The response has a {0} status code."
                (fn [status]
                  (let [actual (:status @response)
                        expected (read-string status)]
                    (tis = expected actual))))

(implement-step "The response body is {0}."
                (fn [body]
                  (tis = body (:body @response))))

(implement-step "The response {0} header is {1}."
                (fn [header-name header-value]
                  (let [header-key (keyword header-name)]
                    (tis = header-value (get-in @response [:headers header-key])))))



(defn -main []
  (info :prose "starting web automation")
  (ready)
  (info :prose "Done reading from tbb. Stopping the server.")
  (server/stop))
