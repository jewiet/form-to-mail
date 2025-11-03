# Submitting Form

## Proof of concept form submission

This is a temporary spec describing a proof of concept for form submission. Its goal is to guide early step of development. In the future it will be modified or removed.

* Given there is a sample form at "http://localhost:1234/poc-form.html"
* the form "action" is set to "http://localhost:8080/poc-submit"
* the form "method" is set to "POST"
* There is a field "email" of type "email"
* There is a field "message" of type "textarea"
* Type "user-one@example.com" in the "email" field
* Type "Hello dear receiver!" in the "message" field
* Click "Send" button
* Form to Mail service will print "Form submitted by user-one@example.com"
* Form to Mail service will print "message: Hello dear receiver!"


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
