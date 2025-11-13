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
* Form to Mail service will log "Sending confirmation link: http://localhost:8080/confirm-submission/<submission-uuid>" 
* Open the confirmation link in the browser.
* There is a message "Thank you for confirmation, your form is delivered".
* Form to Mail service will log "Sending the form <submission-uuid> to publisher-one@example.com".
* Form to Mail service will log "message: Hello dear receiver!"
* There will be a text "Thank you for sending the form. We have sent you an email with confirmation link to user-one@example.com"


## Order form submission

This scenario simulates a complex form with many different inputs. It's a fictional small carpentry website. Form is used to order a dining room set (a table, number of chairs and number of high chairs).

* Serve "spec/samples" on port "1234" 
* Navigate to "http://localhost:1234/order-form.html"
* the form "action" is set to "http://localhost:8080/poc-submit"
* the form "method" is set to "POST"
* There is a field "Street and house number" of type "text"
* There is a field "City" of type "text"
* There is a field "Country" of type "text"
* There is a field "Email" of type "email"
* There is a radio button labeled "Home Delivery"
* There is a radio button labeled "Pickup"
* There is a field "Select type of wood" of type "select"
* There is a field "Select coating" of type "select"
* There is a field "Number of chairs" of type "number"
* There is a field "Number of high chairs" of type "number"
* There is a field "Notes" of type "textarea"
* Type "Verycoolstreet 1" in the "Street and house number" field
* Type "Kittentown" in the "City" field
* Type "Katcountry" in the "Country" field
* Type "szara@muchu.com" in the "Email" field
* Press "Pickup" radio button
* Select "Pine" in the "Select type of wood" field
* Select "Red" in the "Select coating" field
* Type 3 in the "Number of chairs" field
* Type 4 in the "Number of high chairs" field
* Type "Can I pick up the items before christmas?" in the "Notes" field
* Click "Send" button
* Form to Mail service will log "Form submitted by szara@muchu.com" 
* Form to Mail service will log "Sending confirmation link: http://localhost:8080/confirm-submission/<submission-uuid>" 
* Open the confirmation link in the browser.
* There is a message "Thank you for confirmation, your form is delivered".
* Form to Mail service will log "Sending the form <submission-uuid> to publisher-one@example.com" 
* Form to Mail service will log "address: Verycoolstreet 1"
* Form to Mail service will log "city: Kittentown"
* Form to Mail service will log "country: Katcountry"
* Form to Mail service will log "delivery-method: Pickup"
* Form to Mail service will log "wood-type: Pine"
* Form to Mail service will log "coating: Red"
* Form to Mail service will log "chairs: 3"
* Form to Mail service will log "high-chairs: 4" 
* Form to Mail service will log "notes: Can I pick up the items before christmas?" 
* There will be a text "Thank you for sending the form. We have sent you an email with confirmation link to szara@muchu.com"
 

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
