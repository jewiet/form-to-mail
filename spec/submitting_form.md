---
interpreter: clj -J-Dorg.slf4j.simpleLogger.defaultLogLevel=info -M -m interpreters.web-automation
---

# Submitting Form

The tag proof-of-concept denotes a temporary spec describing a proof of concept for form submission. Its goal is to guide early step of development. In the future it will be modified or removed.

The tag covered denotes a spec that is expected to be already implemented, or is currently being implemented.


## Form submission with email and message

``` yaml tbb
tags: [proof-of-concept, covered]
```

* Run the app
* Serve `spec/samples` on port `1234`

  Run the command `miniserve --port 1234 spec/samples`

* Navigate to `http://localhost:1234/poc-form.html`
* There is a `form` element with the following properties

  | name   | value                            |
  |--------|----------------------------------|
  | method | POST                             |
  | action | http://localhost:8080/poc-submit |

* There are the following fields 

  | label              | name    | type     |
  |--------------------|---------|----------|
  | Your email address | email   | email    |
  | Your message       | message | textarea |

  Consider renaming email to sender-email.

* Type `user-one@example.com` in the `Your email address` field
* Type `Hello dear receiver!` in the `Your message` field
* Click `Send` button

* Form to Mail service will log `{:prose "valid form submitted" :by "user-one@example.com"}`
* Open the inbox of `user-one@example.com`
* In the inbox find the message with the subject `Form to Mail confirmation`
* In the message open the link labeled `confirm your submission`
* There is a message `Thank you for confirmation. Your form is delivered.`
* Open the inbox of `publisher-one@example.com`
* In the inbox find the message with the subject `Form to Mail message`
* The message is from `user-one@example.com`
* The message contains `message`


## Order form submission

``` yaml tbb
tags: [proof-of-concept, covered]
```

This scenario simulates a complex form with many different inputs. It's a fictional small carpentry website. Form is used to order a dining room set (a table, number of chairs and number of high chairs).

* Run the app
* Serve `spec/samples` on port `1234` 
* Navigate to `http://localhost:1234/order-form.html`

* There is a `form` element with the following properties

  | name   | value                            |
  |--------|----------------------------------|
  | method | POST                             |
  | action | http://localhost:8080/poc-submit |


* There are the following fields

  | label                   | type     |
  |-------------------------|----------|
  | Street and house number | text     |
  | City                    | text     |
  | Country                 | text     |
  | Email                   | email    |
  | Select type of wood     | select   |
  | Select coating          | select   |
  | Number of chairs        | number   |
  | Number of high chairs   | number   |
  | Notes                   | textarea |

* There is a radio button labeled `Home Delivery`
* There is a radio button labeled `Pickup`

* Type the following input in to the corresponding fields

  | user-input       | input-field             |
  |------------------|-------------------------|
  | Verycoolstreet 1 | Street and house number |
  | Kittentown       | City                    |
  | Katcountry       | Country                 |
  | szara@muchu.com  | Email                   |
  | 3                | Number of chairs        |
  | 4                | Number of high chairs   |


* Click `Pickup` radio button

* Select the following option from the dropdown
 
  | option | dropdown label      |
  |--------|---------------------|
  | Pine   | Select type of wood |
  | Red    | Select coating      |

* Type `Can I pick up the items before christmas?` in the `Notes` field
* Click `Send` button
* Form to Mail service will log `{:prose "valid form submitted" :by "szara@muchu.com"}`
* From a log line matching `{:prose "sending confirmation link"}` extract `:confirmation-url`
* Open the confirmation link in the browser.
* There is a message `Thank you for confirmation. Your form is delivered.`
<!-- * Form to Mail service will log `Sending the form <submission-uuid> to publisher-one@example.com`  -->
* Form to Mail service will log `{"country" "Katcountry", "wood-type" "pine", "city" "Kittentown", "delivery-method" "pickup", "email" "szara@muchu.com", "address" "Verycoolstreet 1", "high-chairs" "4", "coating" "red", "chairs" "3", "notes" "Can I pick up the items before christmas?"}`


## Form submission without email

``` yaml tbb
tags: [proof-of-concept, covered]
```


The email field is the only required field. Submission without email should be rejected.

* Run the app
* Serve `spec/samples` on port `1234` 
* Navigate to `http://localhost:1234/poc-form.html`

* There is a `form` element with the following properties

  | name   | value                            |
  |--------|----------------------------------|
  | method | POST                             |
  | action | http://localhost:8080/poc-submit |

* There are the following fields 

  | label              | name    | type     |
  |--------------------|---------|----------|
  | Your email address | email   | email    |
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

* Run the app
* Navigate to `http://localhost:8080/confirm-submission/8a9de9a5-21a7-4e0a-9cc1-754c0d03abdd`

  The UUID is intentionally wrong.
  
* There is a message `Submission not found`


## Invalid verification url

``` yaml tbb
tags: [proof-of-concept, covered]
```

In this scenario we prove that submission won't be verified unless sender knows its UUID.

<!-- * Follow the steps from `Form submission with email and message` until verification -->

* Run the app
* Navigate to `http://localhost:8080/confirm-submission/bla-bla`

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
