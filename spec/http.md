# Minimal HTTP server

This spec covers direct use of HTTP endpoints, i.e. without a web browser.


## Home page

tags: proof-of-concept covered

- Make a "GET" request to "http://localhost:8080/".

  `http GET :8080/`

- The response has a 200 status code.
- The response body is "Hello, form!".
- The response "content-type" header is "text/plain".
