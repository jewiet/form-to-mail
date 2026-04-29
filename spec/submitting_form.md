---
interpreter: bb spec/interpreters/web_automation.clj
---

# Submitting Form

The tag proof-of-concept denotes a temporary spec describing a proof of concept for form submission. Its goal is to guide early step of development. In the future it will be modified or removed.

The tag covered denotes a spec that is expected to be already implemented, or is currently being implemented.


## Form submission with email and message

``` yaml tbb
tags: [proof-of-concept, covered]
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
    {"1234" {:receiver-name "Publisher One"
             :email-addresses ["publisher-one@example.com"]
             :return-url "http://localhost:1234/thank-you"}}}
  ```
 
* Serve `spec/samples` on port `1234`

  Run the command `miniserve --port 1234 spec/samples`

* Run Mailpit
* Navigate to `http://localhost:1234/poc-form.html`
* There is a `form` element with the following properties

  | name   | value                             |
  |--------|-----------------------------------|
  | method | POST                              |
  | action | http://localhost:8090/submit/1234 |

* There are the following fields 

  | label              | name    | type     |
  |--------------------|---------|----------|
  | Your email address | sender  | email    |
  | Your message       | message | textarea |

* Type `user-one@example.com` in the `Your email address` field
* Type `Hello dear receiver!` in the `Your message` field
* Click `Send` button

* Form to Mail service will log `{:prose "valid form submitted" :by "user-one@example.com"}`
* Navigate to `http://localhost:8025/`
* Open the inbox of `user-one@example.com`
* In the inbox find the message with the subject `Form to Mail confirmation`
* The webpage contains `Before we deliver your form we need to confirm your email address.` 
* In the message open the link labeled `Confirm your submission`
* Open the inbox of `publisher-one@example.com`
* In the inbox find the message with the subject `Form to Mail message`
* The message has reply-to header `user-one@example.com`
* The `text/html` body of the message contains

  <!-- TODO: Format the HTML code and make the interpreter accept it. -->

  ``` html
  <tbody><tr><th scope="row">sender</th><td><ul><li>user-one@example.com</li></ul></td></tr><tr><th scope="row">message</th><td><ul><li>Hello dear receiver!</li></ul></td></tr></tbody>
   ```
  
* The message contains an application/x-www-form-urlencoded encoded attachment with the following fields


  | key     | value                |
  |---------|----------------------|
  | sender  | user-one@example.com |
  | message | Hello dear receiver! |


## Order form submission

``` yaml tbb
tags: [proof-of-concept, covered, focus]
```

This scenario simulates a complex form with many different inputs. It's a fictional small carpentry website. Form is used to order a dining room set (a table, number of chairs and number of high chairs).

* Run the app with the following configuration


  ``` clojure
  {:smtp-server    {:host "localhost"
                    :port 1025
                    :ssl  false}
   :from-address   "form-to-mail@localhost"
   :base-url       "http://localhost:8090"
   :listen-address "127.0.0.1"
   :listen-port    8090
   :receivers      {"2345" {:receiver-name "Publisher Two"
                            :email-addresses ["publisher-two@example.com"]
                            :return-url "http://localhost:1234/thank-you"}}}
  ```

* Serve `spec/samples` on port `1234` 
* Run Mailpit
* Navigate to `http://localhost:1234/order-form.html`

* There is a `form` element with the following properties

  | name   | value                             |
  |--------|-----------------------------------|
  | method | POST                              |
  | action | http://localhost:8090/submit/2345 |


* There are the following fields

  | label                   | type     | name            |
  |-------------------------|----------|-----------------|
  | Street and house number | text     | address         |
  | City                    | text     | city            |
  | Country                 | text     | country         |
  | Email                   | email    | sender          |
  | Select type of wood     | select   | wood-type       |
  | Select coating          | select   | coating         |
  | Number of chairs        | number   | chairs          |
  | Number of high chairs   | number   | high-chairs     |
  | Notes                   | textarea | notes           |
  | Home Delivery           | radio    | delivery-method |
  | Pickup                  | radio    | delivery-method |

* Enter the following input in to the corresponding fields

  | user-input                                | label                   |
  |-------------------------------------------|-------------------------|
  | Verycoolstreet 1                          | Street and house number |
  | Kittentown                                | City                    |
  | Katcountry                                | Country                 |
  | szara@muchu.com                           | Email                   |
  | 3                                         | Number of chairs        |
  | 4                                         | Number of high chairs   |
  | Pine                                      | Select type of wood     |
  | Red                                       | Select coating          |
  | Can I pick up the items before christmas? | Notes                   |


* Click `Pickup` radio button
* Click `Send` button
* Form to Mail service will log `{:prose "valid form submitted" :by "szara@muchu.com"}`
* Navigate to `http://localhost:8025/`
* Open the inbox of `szara@muchu.com`
* In the inbox find the message with the subject `Form to Mail confirmation`
* The webpage contains `Before we deliver your form we need to confirm your email address.`
* In the message open the link labeled `Confirm your submission`
* Open the inbox of `publisher-two@example.com`
* In the inbox find the message with the subject `Form to Mail message`
* The message has reply-to header `szara@muchu.com`
* The `text/html` body of the message contains

  <!-- TODO: Format the HTML code and make the interpreter accept it. -->

  ``` html
  <tbody><tr><th scope="row">address</th><td><ul><li>Verycoolstreet 1</li></ul></td></tr><tr><th scope="row">city</th><td><ul><li>Kittentown</li></ul></td></tr><tr><th scope="row">high-chairs</th><td><ul><li>4</li></ul></td></tr><tr><th scope="row">wood-type</th><td><ul><li>pine</li></ul></td></tr><tr><th scope="row">chairs</th><td><ul><li>3</li></ul></td></tr><tr><th scope="row">notes</th><td><ul><li>Can I pick up the items before christmas?</li></ul></td></tr><tr><th scope="row">sender</th><td><ul><li>szara@muchu.com</li></ul></td></tr><tr><th scope="row">coating</th><td><ul><li>red</li></ul></td></tr><tr><th scope="row">delivery-method</th><td><ul><li>pickup</li></ul></td></tr><tr><th scope="row">country</th><td><ul><li>Katcountry</li></ul></td></tr></tbody>
  ```
  
* The message contains an application/x-www-form-urlencoded encoded attachment with the following fields


  | key             | value                                     |
  |-----------------|-------------------------------------------|
  | sender          | szara@muchu.com                           |
  | country         | Katcountry                                |
  | wood-type       | pine                                      |
  | city            | Kittentown                                |
  | delivery-method | pickup                                    |
  | address         | Verycoolstreet 1                          |
  | high-chairs     | 4                                         |
  | coating         | red                                       |
  | chairs          | 3                                         |
  | notes           | Can I pick up the items before christmas? |


## Form submission without email

``` yaml tbb
tags: [proof-of-concept, covered]
```


The email field is the only required field. Submission without email should be rejected.

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
    {"1234" {:receiver-name "Publisher One"
             :email-addresses ["publisher-one@example.com"]
             :return-url "http://localhost:1234/thank-you"}}}
  
  ```

* Serve `spec/samples` on port `1234` 
* Navigate to `http://localhost:1234/poc-form.html`

* There is a `form` element with the following properties

  | name   | value                             |
  |--------|-----------------------------------|
  | method | POST                              |
  | action | http://localhost:8090/submit/1234 |

* There are the following fields 

  | label              | name    | type     |
  |--------------------|---------|----------|
  | Your email address | sender  | email    |
  | Your message       | message | textarea |

* Type `Hello dear receiver!` in the `Your message` field
* Click `Send` button
* There is a message `Missing required field email`
* Form to Mail service will log `{:prose "Missing required field" :field "email"}`


## Wrong verification url

``` yaml tbb
tags: [proof-of-concept, covered]
```

In this scenario we prove that submission won't be verified unless sender knows its UUID.

<!-- * Follow the steps from `Form submission with email and message` until verification -->

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
    {"1234" {:receiver-name "Publisher One"
             :email-addresses ["publisher-one@example.com"]
             :return-url "http://localhost:1234/thank-you"}}}
  ```

* Navigate to `http://localhost:8090/confirm-submission/8a9de9a5-21a7-4e0a-9cc1-754c0d03abdd`

  The UUID is intentionally wrong.
  
* There is a message `Submission not found`


## Invalid verification url

``` yaml tbb
tags: [proof-of-concept, covered]
```

In this scenario we prove that submission won't be verified unless sender knows its UUID.

<!-- * Follow the steps from `Form submission with email and message` until verification -->

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
    {"1234" {:receiver-name "Publisher One"
             :email-addresses ["publisher-one@example.com"]
             :return-url "http://localhost:1234/thank-you"}}}
  ```

* Navigate to `http://localhost:8090/confirm-submission/bla-bla`

  The UUID is intentionally invalid.
  
* There is a message `Invalid submission uuid`

<!-- 

## Successful form submission

tags: not-implemented

This is how the form submission should work once the project is complete.

- Given that the form is registered by `publisher-one@example.com` with ID `:uuid`.
- Navigate to `https://publisher-one.com/contact-us`.
- Enter `user-one@example.com` into the `Email Address` input field of type `email`.
- Enter `Renew subscription` in to the `Subject` input field of type `text`.
- Enter `My current plan expires...` in to the `Message` input field of type `text` .
- Click the `Submit` button.
- There is a message `Thank you for contacting us! We have sent an email to user-one@example.com for confirmation`.
- User receives an email at `user-one@example.com` with a link labeled `Confirm`.
- Click the `Confirm` link..
- There is a message `Thank you for confirmation, your email is being delivered to Publisher One`.
- Publisher receives an email at `publisher-one@example.com` with a subject `Renew subscription` from `user-one@example.com` with a message `My current plan expires...`. 

 -->

## Port and interface can be changed through config
