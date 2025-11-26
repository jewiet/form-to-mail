---
interpreter: clj -J-Dorg.slf4j.simpleLogger.defaultLogLevel=info -M -m interpreters.web-automation
---

# Create form

- Given that the publisher is logged in using publisher-one@example.com.
- Navigate to "/create-form".
- Enter "Contact Form" into the "Form Name" input field of type "text".
- Click the "Create" button.
- There is a message "Please set the value of the action attribute of your form to https://form-to-mail.com/:uuid".
