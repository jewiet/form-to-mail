---
interpreter: bb spec/interpreters/http.clj
---


# Minimal HTTP server

This spec covers direct use of HTTP endpoints, i.e. without a web browser.


## Home page

* Run the app with the following configuration

  ``` clojure
  {:from-address   "form-to-mail@localhost"
   :base-url       "http://localhost:8080"
   :listen-address "127.0.0.1"
   :listen-port    8080
   :receivers
    {"1234" {:receiver-name "Alice Enterprises"
             :email-addresses ["hello@alice.org"]
             :return-url "http://localhost:8080/thank-you"}}}
  ```

- Make a `GET` request to `http://localhost:8080/`.

  `http GET :8080/`

- The response has a `404` status code.
- The response `content-type` header is `text/plain`.


## Form submission without email

* Run the app with the following configuration

  ``` clojure
  {:from-address   "form-to-mail@localhost"
   :base-url       "http://localhost:8080"
   :listen-address "127.0.0.1"
   :listen-port    8080
   :receivers
    {"something-else" {:receiver-name "Bobs Corp"
                       :email-addresses ["info@bobs.com"]
                       :return-url "http://localhost:1234/thank-you"}}}
  ```

- Make a `POST` request to `http://localhost:8080/submit/something-else`.

- The response has a `422` status code.
- The response body is `Missing required field sender`.
- The response `content-type` header is `text/plain`.
