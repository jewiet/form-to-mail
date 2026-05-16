(ns app.templates
  (:require
   [clojure.java.io :as io]
   [hiccup2.core :as hiccup]))

(defn multipart-error-page []
  (let [html-head              [:head
                                [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                                [:link {:type "text/css", :href "/pico.min.css", :rel "stylesheet"}]
                                [:link {:type "text/css", :href "/style.css", :rel "stylesheet"}]]
        html-header            [:header.container
                                [:img#main-logo {:src "/logo.svg" :alt "Form to Mail logo"}]
                                [:hgroup
                                 [:h1 "Form to Mail"]
                                 [:p "Web forms for static websites"]]]
        html-main              [:main {:class "container"}
                                [:p "Sorry! The form on this website is not set up correctly. As a result the content you submitted won't be delivered."]
                                [:h2 "Technical Explanation"]
                                [:p "The explanation below is for the form developer. If you are trying to submit a form on somebody else's website there is unfortunately nothing you can do. It's not your fault and we are sorry for the inconvenience."]
                                [:p "The form was submitted using multipart/form-data encoding. Currently Form to Mail only supports URL encoded forms, i.e. application/x-www-form-urlencoded, which is the default. Simply remove the enctype attribute from your form."]
                                [:p "Are you trying to set up a file upload? This is currently not supported by Form to Mail."]
                                [:a {:href "https://github.com/jewiet/form-to-mail/"}
                                 "Read more here"]]

        html-body              [:body
                                html-header
                                html-main]]
    (str (hiccup/raw "<!doctype html>")
         (hiccup/html [:html {:lang "en" :class "submission app"}
                       html-head
                       html-body]))))

(defn submission [email]
  (let [html-head              [:head
                                [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                                [:link {:type "text/css", :href "/pico.min.css", :rel "stylesheet"}]
                                [:link {:type "text/css", :href "/style.css", :rel "stylesheet"}]]
        html-header            [:header.container
                                [:img#main-logo {:src "/logo.svg" :alt "Form to Mail logo"}]
                                [:hgroup
                                 [:h1 "Form to Mail"]
                                 [:p "Web forms for static websites"]]]
        html-main              [:main {:class "container"}
                                [:p "Thank you for sending the form. Before we deliver it we need to verify your email address. We have sent you a link to:"]
                                [:p [:img.icon {:src "/streamline-emojis--open-mailbox-with-raised-flag.svg"}] [:strong email]]
                                [:p "Please follow the verification link in the email to complete the delivery."]]

        html-body              [:body
                                html-header
                                html-main]]
    (str (hiccup/raw "<!doctype html>")
         (hiccup/html [:html {:lang "en" :class "submission app"}
                       html-head
                       html-body]))))

;; TODO: DRY with submission
(defn confirmation [{:keys [return-url receiver-name]}]
  (let [html-head              [:head
                                [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                                [:link {:type "text/css", :href "/pico.min.css", :rel "stylesheet"}]
                                [:link {:type "text/css", :href "/style.css", :rel "stylesheet"}]]
        html-header            [:header.container
                                [:img#main-logo {:src "/logo.svg" :alt "Form to Mail logo"}]
                                [:hgroup
                                 [:h1 "Form to Mail"]
                                 [:p "Web forms for static websites"]]]
        html-main              [:main {:class "container"}
                                [:p
                                 "Thank you for confirming your email address. "
                                 "Your form is delivered to "
                                 [:strong receiver-name]
                                 ". Our work is done. It is up to them to "
                                 "respond to you via email. In a few seconds you "
                                 "shall be redirected to:"]
                                [:p
                                 [:img.icon {:src "/streamline-emojis--backhand-index-pointing-right-1.svg"}]
                                 [:a {:href return-url} return-url]]]

        html-body              [:body
                                html-header
                                html-main]]
    (str (hiccup/raw "<!doctype html>")
         (hiccup/html [:html {:lang "en" :class "submission app"}
                       html-head
                       html-body]))))

(defn confirmation-email-html [confirmation-url]
  [{:type "text/html"
    :content (str (hiccup/html [:html
                                [:head
                                 [:style (slurp (io/resource "public/mail.css"))]]
                                [:body
                                 [:div#logo (hiccup/raw (slurp (io/resource "public/logo.svg")))]
                                 [:h1 "Form to Mail"]
                                 [:p [:strong "confirmation"]]
                                 [:p
                                  "Thank you for submitting the form via "
                                  [:a {:href "https://formtomail.eu/"} "Form to Mail"]
                                  ". Before we deliver your form we need to confirm your email address. Please click below."]
                                 [:p
                                  [:a {:href confirmation-url :class "call-to-action"} "Confirm your submission"]
                                  [:p "If you haven't filled the form please ignore this email."]]]]))}])

(defn- value->html [v]
  (if (vector? v)
    (map value->html v)
    [:li v]))

(defn delivery-email-html [parsed raw]
  [{:type    "text/html"
    :content (str (hiccup/html [:html
                                [:head
                                 [:style (slurp (io/resource "public/mail.css"))]]
                                [:body.container
                                 [:div#logo (hiccup/raw (slurp (io/resource "public/logo.svg")))]
                                 [:h1 "Form to Mail"]
                                 [:p [:strong "Form delivery"]]
                                 [:p "Contents of the form submitted"]
                                 [:table
                                  [:tbody
                                   (map (fn [[k v]]
                                          [:tr
                                           (list [:th {:scope "row"} (name k)]
                                                 [:td [:ul (value->html v)]])])
                                        parsed)]]]]))}

   {:type         :attachment
    :file-name    "form-to-mail-request-body.txt"
    :content-type "application/x-www-form-urlencoded"
    :content      (.getBytes raw)}])

(defn submission-not-found []
  (let [html-head              [:head
                                [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                                [:link {:type "text/css", :href "/pico.min.css", :rel "stylesheet"}]
                                [:link {:type "text/css", :href "/style.css", :rel "stylesheet"}]]
        html-header            [:header.container
                                [:img#main-logo {:src "/logo.svg" :alt "Form to Mail logo"}]
                                [:hgroup
                                 [:h1 "Form to Mail"]
                                 [:p "Web forms for static websites"]]]
        html-main              [:main {:class "container"}
                                [:p
                                 "We can't find this submission. Sorry!"]

                                [:p
                                 "Perhaps the submission expired. "
                                 "Did you send the form more than 30 minutes ago? "
                                 "For privacy reasons we do not store unconfirmed submissions longer than that."]]

        html-body              [:body
                                html-header
                                html-main]]
    (str (hiccup/raw "<!doctype html>")
         (hiccup/html [:html {:lang "en" :class "submission-not-found app"}
                       html-head
                       html-body]))))
