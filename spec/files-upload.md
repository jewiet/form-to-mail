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
