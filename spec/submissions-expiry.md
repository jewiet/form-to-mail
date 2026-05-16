---
interpreter: bb spec/interpreters/web_automation.clj
---

# Submissions expiry

## Submissions not confirmed within half an hour expire

* Run the app with the following configuration

  ``` clojure
  {:smtp-server    {:host "localhost"
                    :port 1025
                    :ssl  false}
   :from-address   "form-to-mail@localhost"
   :base-url       "http://localhost:8090"
   :listen-address "127.0.0.1"
   :listen-port    8090
   :receivers
    {"1234" {:receiver-name "Bobby Bob"
             :email-addresses ["bob@example.com"]
             :return-url "http://localhost:1234/thank-you"}}}
  ```

* Serve `spec/samples` on port `1234`
* Run Mailpit
* // Fill the `basic-form.html` as `alice@example.com` with the following data

  | field        | value      |
  |--------------|------------|
  | Your message | Hello Bob! |

* Navigate to `http://localhost:1234/poc-form.html`
* Type `alice@example.com` in the `Your email address` field
* Type `Hello dear receiver!` in the `Your message` field
* Click `Send` button

* // Expect the correct confirmation prompt for `alice@example.com`

* The webpage contains `Thank you for sending the form. Before we deliver it we need to verify your email address. We have sent you a link to:`
* The webpage contains `alice@example.com`
* The webpage contains `Please follow the verification link in the email to complete the delivery.`

* Wait `45 minutes`

* Navigate to `http://localhost:8025/`
* Open the inbox of `alice@example.com`
* In the inbox find the message with the subject `Form to Mail confirmation`
* The webpage contains `Before we deliver your form we need to confirm your email address.` 
* In the message open the link labeled `Confirm your submission`
* The webpage contains `We can't find this submission. Sorry!` 
* The webpage contains `Perhaps the submission expired. Did you send the form more than 30 minutes ago? For privacy reasons we do not store unconfirmed submissions longer than that.` 


## Submissions can be confirmed within 30 minutes

``` yaml tbb
tags:
- focus
```

* Run the app with the following configuration

  ``` clojure
  {:smtp-server    {:host "localhost"
                    :port 1025
                    :ssl  false}
   :from-address   "form-to-mail@localhost"
   :base-url       "http://localhost:8090"
   :listen-address "127.0.0.1"
   :listen-port    8090
   :receivers
    {"1234" {:receiver-name "Bobby Bob"
             :email-addresses ["bob@example.com"]
             :return-url "http://localhost:1234/thank-you"}}}
  ```

* Serve `spec/samples` on port `1234`
* Run Mailpit
* // Fill the `basic-form.html` as `alice@example.com` with the following data

  | field        | value      |
  |--------------|------------|
  | Your message | Hello Bob! |

* Navigate to `http://localhost:1234/poc-form.html`
* Type `alice@example.com` in the `Your email address` field
* Type `Hello dear receiver!` in the `Your message` field
* Click `Send` button

* // Expect the correct confirmation prompt for `alice@example.com`

* The webpage contains `Thank you for sending the form. Before we deliver it we need to verify your email address. We have sent you a link to:`
* The webpage contains `alice@example.com`
* The webpage contains `Please follow the verification link in the email to complete the delivery.`

* Wait `25 minutes`

* Navigate to `http://localhost:8025/`
* Open the inbox of `alice@example.com`
* In the inbox find the message with the subject `Form to Mail confirmation`
* The webpage contains `Before we deliver your form we need to confirm your email address.` 

* In the message open the link labeled `Confirm your submission`

TODO: Find a way to assert text acros multiple elements (i.e. don't chop it like that). Something like: `The webpage contains the following text`.

Also, use similar assertions for other confirmation scenarios.

* The webpage contains `Thank you for confirming your email address. `
* The webpage contains `Your form is delivered to `
* The webpage contains `Bobby Bob`
* The webpage contains `In a few seconds you shall be redirected to:`
* The webpage contains `http://localhost:1234/thank-you`

* Open the inbox of `bob@example.com`
* In the inbox find the message with the subject `Form to Mail message`
* The message has reply-to header `alice@example.com`

## Confirmed submissions are deleted after 24 hours

Reconfirming within 24 hours gives appropriate explanation, i.e. not 404.
