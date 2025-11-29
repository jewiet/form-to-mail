(ns interpreters.http
  (:require
   [app.server :as server]
   [clojure.string :refer [lower-case]]
   [io.pedestal.log :refer [debug info spy]]
   [org.httpkit.client :as hk-client]
   [tbb.core :refer [implement-step ready tis]]))

(defonce response (atom nil))

(implement-step "Run the app"
                (fn [_]
                  (server/start)))

(implement-step  "Make a {0} request to {1}."
                 (fn [method url _]
                   (debug :prose "making http request" :method method :url url)
                   (let [method (keyword (lower-case method))]
                     (-> {:method method
                          :url url}
                         hk-client/request
                         deref
                         (#(reset! response %))
                         (spy)))))

(implement-step "The response has a {0} status code."
                (fn [status _]
                  (let [actual (:status @response)
                        expected (read-string status)]
                    (tis = expected actual))))

(implement-step "The response body is {0}."
                (fn [body _]
                  (tis = body (:body @response))))

(implement-step "The response {0} header is {1}."
                (fn [header-name header-value _]
                  (let [header-key (keyword header-name)]
                    (tis = header-value (get-in @response [:headers header-key])))))



(defn -main []
  (debug :prose "starting http interpreter")
  (ready)
  (debug :prose "Stopping the server.")
  (server/stop))
