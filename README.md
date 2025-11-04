# Form to mail service

This service is designed to handle data submitted from an HTML form on a website and securely send that information to an email address.

## Run the program

``` shell
clojure -M -m app.core
```


## Build uberjar

``` shell
clj -T:build uber
```


## Run the uberjar

``` shell
java -jar target/form-to-mail-0.1.19-standalone.jar
```

## Roadmap

- [x] Create Repository
- [x] Write a README.md
- [x] Write a spec
  - [x] Publisher registration
  - [x] Publisher login
  - [x] Form creation
  - [x] Form submitting

- [x] Write a spec for Clojure HTTP server
- [x] Setup Clojure program
- [x] Implement Clojure HTTP server - a simple HTTP server that responds to a get request with a hello world.
- [x] Setup a development environment using Nix
- [x] Run the app on local machine
- [x] Setup VPS on Hetzner with Nix
- [x] Create Babashkla tasks to run and build the application
- [x] Deploy form to mail to the server
  - [x] Setup Systemd service
  - [x] Run it as a dedicated user
  - [x] Deploy using Nix

- [x] Implement proof of concept for form submission. See <spec/submitting_form.md> 
- [x] Add clj-nix#deps-lock to development environment
- [ ] Improve PoC spec
  - [x] The email field is required
  - [ ] Handle any other field
- [ ] Implement publisher registration form
- [ ] Setup CI pipeline
- [ ] Convert spec to Gauge
- [ ] Research transaction email provider
  - [ ] must be European
  - [ ] cheap

