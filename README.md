# Form to Mail

Form to Mail is a lightweight web service that sends data submitted from a standard HTML form as an email via any SMTP server. It's designed to work with static websites and easy to self-host.

## Try it out on your laptop

You will need:

1. A Mac, Linux, BSD or similar computer
2. Java or Nix with flakes
3. Three email accounts (Sender, Receiver and Service)

We assume you will use your existing email account to receive emails. For example alice@example.com.

Follow these steps:

Step 1. Create a new email account for the sender

A sender is a person who submits a form. For this test we ask you to create separate accounts to pretend there are two people communicating. Let's call it bob@example.com.

Step 2. Create a new email account for the service

The service account will be used to send confirmation emails to senders and deliver contents of a form to a receiver. Something like noreply@example.com.

Step 3. Wrte a configuration file

Create a file called `form-to-mail.edn` with the following content. 

``` clojure
{:smtp-server    {:host "smtp.example.com"
                  :port 587
                  :user "formtomail@example.com"
                  :pass "your-smtp-server-password"
                  :ssl  true}
 :from-address   "noreply@example.com"
 :listen-address "127.0.0.1"
 :base-url       "http://localhost:8080"
 :listen-port    8080
 :receivers      {"1234" "alice@example.com"}}
```

Notice that you can have multiple receivers. The key `"1234"` is an identifier. You can type there whatever you want as long as it's unique. 


Step 4. Create an HTML file with a standard form

Create a new HTML file called `form.html` with the following content.

``` html
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Contact Us</title>
</head>
<body>
<form action="http://localhost:8080/submit/1234" method="POST">
    <label>
      E‑mail:
      <input type="email" name="email" required>
    </label>

    <label>
      Message:
      <textarea name="message"></textarea>
    </label>

    <button type="submit">Send</button>
  </form>
</body>
</html>
```
 
Notice that the value of the action is constructed using the `:base-url` value and the identifier of the receiver from the `form-to-mail.edn` configuration file. 


Step 5. Start Form to Mail service

Currently it's distributed as a single uberjar or a Nix flake.

a. Download the [Uberjar](https://github.com/jewiet/form-to-mail/releases) file

An uberjar is a Java archive that contains a program and all it's dependencies.

Execute this command in your terminal `java -jar form-to-mail.x.x.x.jar form-to-mail.edn`

WIP: GitHub Actions to build and publish the uberjar are not yet implemented.


b. Execute this command in your terminal `Nix run github:jewiet/form-to-mail -- form-to-mail.edn`

You need `Nix` with [flakes](https://nixos.wiki/wiki/flakes) enabled.


Step 6.  Fill and submit the form 

Email field is required E.g Enter bob@example.com in the email field and submit the form.

Form to Mail will log "Thank you for sending the form. We have sent you an email with confirmation link to bob@example.com"


Step 7. Check your sender's inbox for a confirmation email with a link

Follow the link. Form to Mail will log "Thank you for confirmation. Your form is delivered."
 

Step 8. Check your receiver's inbox for an email from Form to Mail
  
The email should contain the form's content, sender's email address, an attachment with the original request body and reply-to is set to sender's email address.


## Deploy it on your server

WIP: Instructions will follow soon.

## Contribute

Contributions of any kind are welcome.

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
  - [x] Handle any other field
  - [ ] TODO: Preserve order of params
- [ ] Implement publisher registration form
- [ ] Persistence
- [x] Email delivery
- [ ] Setup CI pipeline
- [ ] Convert spec to Tad Better Behavior
  - [ ] create_form.md
  - [x] http.md
  - [ ] publisher_login.md
  - [ ] publisher_registration.md
  - [x] submitting_form.md

- [x] Research transaction email provider
  - [x] must be European
  - [x] cheap
- [x] Improve logging
    - [x] Log to stderr
    - [x] Use logging library
    - [x] Allow changing the log level
