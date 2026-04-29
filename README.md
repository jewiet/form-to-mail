# Form to Mail

Form to Mail is a lightweight web service that sends data submitted from a standard HTML form as an email via any SMTP server. It's designed to work with static websites and for easy self-hosting.


## Try it out on your laptop

You will need:

1. A Mac, Linux, BSD or similar computer
2. Java or Nix with flakes
3. Three email accounts (Sender, Receiver and Service)

We assume you will use your existing email account to receive emails. For example alice@example.com.

Follow these steps:


### Step 1. Create a new email account for the sender

A sender is a person who submits a form. For this test we ask you to create separate accounts to pretend there are two people communicating. Let's call it bob@example.com.


### Step 2. Create a new email account for the service

The service account will be used to send confirmation emails to senders and deliver contents of a form to a receiver. Something like noreply@example.com.


### Step 3. Wrte a configuration file

Create a file called `form-to-mail.edn` with the following content. 

``` clojure
{:smtp-server    {:host "smtp.example.com"
                  :port 587
                  :user "noreply@example.com"
                  :pass "your-smtp-server-password"
                  :ssl  true}
 :from-address   "noreply@example.com"
 :listen-address "127.0.0.1"
 :base-url       "http://localhost:8080"
 :listen-port    8080
 :receivers      {"1234" {:receiver-name "Alice"
                          :email-addresses ["alice@example.com"]
                          :return-url "https://alice-wonderland/thank-you"}}}
```

Notice that you can have multiple receivers. The key `"1234"` is an identifier. You can type there whatever you want as long as it's unique. 


### Step 4. Create an HTML file with a standard form

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
        <input type="email" name="sender" required>
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


### Step 5. Start Form to Mail service

Currently it's distributed as a single uberjar or a Nix flake. You can chose the method you like.


#### The uberjar method:

An uberjar is a Java archive that contains a program and all its dependencies. All you need is a working Java runtime environment (JRE).
   
[Download the jar file](https://github.com/jewiet/form-to-mail/releases) and execute the following command in your terminal:

``` shell
java -jar form-to-mail.x.x.x.jar form-to-mail.edn
```


#### The Nix method

You'll need Nix with [flakes](https://nixos.wiki/wiki/flakes) enabled, but you don't need to download anything. Just run:
    
``` shell
nix run github:jewiet/form-to-mail -- form-to-mail.edn
```


### Step 6. Fill and submit the form as Bob

Enter bob@example.com in the email field and write something nice in the message input. Submit the form. You should see a page prompting you to confirm your email address. 


### Step 7. Confirm Bob's email

Check the inbox of Bob (the sender) for a confirmation email with a link. Follow the link. There should be a page confirming the form being delivered.
 

### Step 8. Alice gets an email from Bob

Check the inbox of Alice (the receiver) for an email from Bob via Form to Mail. The email should contain the form's content (including the verified sender's email address), an attachment with the original form data. The reply-to header should be set to Bob's email address, so Alice can reply directly to Bob and take the conversation from here.


## Deploy it on your server

WIP: Instructions will follow soon.


## Contribute

Contributions of any kind are welcome.
