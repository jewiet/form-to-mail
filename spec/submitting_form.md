# Submitting Form

The tag proof-of-concept denotes a temporary spec describing a proof of concept for form submission. Its goal is to guide early step of development. In the future it will be modified or removed.

The tag covered denotes a spec that is expected to be already implemented, or is currently being implemented.


## Form submission with email and message

tags: proof-of-concept covered

* Serve "spec/samples" on port "1234" 

  Run the command `miniserve --port 1234 spec/samples`

* Navigate to "http://localhost:1234/poc-form.html"
* the form "action" is set to "http://localhost:8080/poc-submit"

  Query for `form`, e.g. `$("form")`.

* the form "method" is set to "POST"
* There is a field "email" of type "email"

  Query for `[name="email"]`. Check the type attribute.

* There is a field "message" of type "textarea"

  Query for `[name="message"]`. Check the element name.

* Type "user-one@example.com" in the "email" field
* Type "Hello dear receiver!" in the "message" field
* Click "Send" button
* Form to Mail service will log "Form submitted by user-one@example.com"
* Form to Mail service will log "message: Hello dear receiver!"
* There will be a text "Thank you for sending the form. We have sent you an email with confirmation link to user-one@example.com"

## Form submission without email

tags: proof-of-concept covered

The email field is the only required field. Submission without email should be rejected.

* Serve "spec/samples" on port "1234" 
* Navigate to "http://localhost:1234/poc-form.html"
* the form "action" is set to "http://localhost:8080/poc-submit"
* the form "method" is set to "POST"
* There is a field "email" of type "email"
* There is a field "message" of type "textarea"
* Type "Hello dear receiver!" in the "message" field
* Click "Send" button
* There will be a text "Missing required field email"
* Form to Mail service will log "Missing required field email"


## Successful form submission

tags: not-implemented

This is how the form submission should work once the project is complete.

- Given that the form is registered by "publisher-one@example.com" with ID ":uuid".
- Navigate to "https://publisher-one.com/contact-us".
- Enter "user-one@example.com" into the "Email Address" input field of type "email".
- Enter "Renew subscription" in to the "Subject" input field of type "text".
- Enter "My current plan expires..." in to the "Message" input field of type "text" .
- Click the "Submit" button.
- There is a message "Thank you for contacting us! We have sent an email to user-one@example.com for confirmation".
- User receives an email at "user-one@example.com" with a link labeled "Confirm".
- Click the "Confirm" link..
- There is a message "Thank you for confirmation, your email is being delivered to Publisher One".
- Publisher receives an email at "publisher-one@example.com" with a subject "Renew subscription" from "user-one@example.com" with a message "My current plan expires...". 
