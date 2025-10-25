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

### Day 1
- [x] Create Repository
- [x] Write a README.md
- [x] Write a spec
     - [x] Publisher registration
     - [x] Publisher login
     - [x] Form creation
     - [x] Form submitting

### Day 2

- [x] Write a spec for Clojure HTTP server
- [x] Setup Clojure program
- [x] Implement Clojure HTTP server - a simple HTTP server that responds to a get request with a hello world.
- [x] Setup a development environment using Nix
- [x] Run the app on local machine

### Day 3

- [ ] Setup VPS on Hetzner with Nix
- [ ] Deploy form to mail to the server

### Day 4

- [ ] Setup CI pipeline
- [ ] Convert spec to Gauge


### Day 5

- [ ] Start coding the program
- [ ] Implement publisher registration form
- [ ] Research transaction email provider
    - [ ] must be European 
    - [ ] cheap

