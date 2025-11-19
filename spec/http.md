---
interpreter: clj -M -m interpreters.http
---


# Minimal HTTP server

This spec covers direct use of HTTP endpoints, i.e. without a web browser.


## Home page

tags: proof-of-concept covered

- Run the app
- Make a `GET` request to `http://localhost:8080/`.

  `http GET :8080/`

- The response has a `200` status code.
- The response body is `Hello, form!`.
- The response `content-type` header is `text/plain`.


## Form submission without email

tags: proof-of-concept covered

- Run the app
- Make a `POST` request to `http://localhost:8080/poc-submit`.

  `http --form POST http://localhost:8080/poc-submit`

- The response has a `422` status code.
- The response body is `Missing required field email`.
- The response `content-type` header is `text/plain`.
