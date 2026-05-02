---
interpreter: bb spec/interpreters/web_automation.clj
---

# Files upload

Form to Mail doesn't support uploading files by design. In particular requests with multi-part encoding are rejected. This is because of security and legal concerns. It's important that users are aware of that. In this regard we have four classes of users.

1. A sender - lets call her Alice
2. A recipient called Bob
3. An author of the form called Charlie
4. A server administrator - Dami

When sending a form with multi-part encoding Alice needs to know:

1. That the form won't be delivered
2. That it's not her fault

When developing the website with the form Charlie needs to know that multi-part forms are not supported. This is explained in documentation but we also need a clear error message in case he missed it.

In case Charlie built a form with multi-part encoding, Bob needs to know the form is not properly set up, so he can ask Charlie to fix it.

If there is a lot of rejected submissions, Dami should see it in the logs so she can notify Bob or Charlie.

We assume that Charlie and Bob will test their form at least once by submitting it. So we can use the same error page that Alice would see, to explain the technical reason. In case they published a faulty form, Alice should see a friendly and apologetic error message.

## A form with multipart encoding will be rejected

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
    {"1234" {:receiver-name "Bob Carpenter"
             :email-addresses ["bob@example.com"]
             :return-url "http://localhost:1234/thank-you"}}}
  ```
 
* Serve `spec/samples` on port `1234`

  Run the command `miniserve --port 1234 spec/samples`

* Run Mailpit

* Navigate to `http://localhost:1234/file-uploads.html`
* There is a `form` element with the following properties

  | name    | value                             |
  |---------|-----------------------------------|
  | method  | POST                              |
  | action  | http://localhost:8090/submit/1234 |
  | enctype | multipart/form-data               |

* There are the following fields 

  | label              | name    | type     |
  |--------------------|---------|----------|
  | Your email address | sender  | email    |
  | Your message       | message | textarea |
  | Choose a file      | avatar  | file     |

* Type `alice@example.com` in the `Your email address` field
* Type `Hello Bob! This is my cat.` in the `Your message` field
* Load `spec/samples/squirrel.png` in the `Choose a file` field
* Click `Send` button
* The webpage contains the following

  ```text
  Sorry! The form on this website is not set up correctly. As a result the content you submitted won't be delivered.
  ```

* The webpage contains `Technical Explanation`

  It's a heading.

* The webpage contains the following

  ```text
  The explanation below is for the form developer. If you are trying to submit a form on somebody else's website there is unfortunately nothing you can do. It's not your fault and we are sorry for the inconvenience.
  ```

  ```text
  The form was submitted using multipart/form-data encoding. Currently Form to Mail only supports URL encoded forms, i.e. application/x-www-form-urlencoded, which is the default. Simply remove the enctype attribute from your form.
  ```

  ```text
  Are you trying to set up a file upload? This is currently not supported by Form to Mail.
  ```

* There is a link `Read more here` to `https://github.com/jewiet/form-to-mail/`
* Form to Mail service will log `{:prose "invalid form submitted" :reason "multipart/form-data encoding is not supported"}`

  TODO: There should also be an address of the website with the faulty form (referrer)

* Navigate to `http://localhost:8025/`
* There are no messages in the inbox of `alice@example.com`
