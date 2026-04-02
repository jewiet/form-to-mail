(ns app.templates
  (:require
   [clojure.java.io :as io]
   [hiccup2.core :as hiccup]))

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
