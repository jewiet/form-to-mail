# Roadmap

- [x] Create Repository
- [x] Write a README.md
- [x] Write a spec
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
  - [x] Handle any other field
  - [ ] TODO: Preserve order of params
- [ ] Persistence
- [x] Email delivery
- [x] Setup CI pipeline
- [x] Convert spec to Tad Better Behavior
  - [x] http.md
  - [x] submitting_form.md

- [x] Research transaction email provider
  - [x] must be European
  - [x] cheap
- [x] Improve logging
    - [x] Log to stderr
    - [x] Use logging library
    - [x] Allow changing the log level
