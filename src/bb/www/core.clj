(ns www.core
  (:require
   [babashka.fs :as fs]
   [hiccup2.core :as hiccup]))

(def index-html
  (let [html-head              [:head
                                [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                                [:link {:type "text/css", :href "/pico.min.css", :rel "stylesheet"}]
                                [:link {:type "text/css", :href "/style.css", :rel "stylesheet"}]]
        html-header            [:header.container
                                [:nav
                                 [:ul
                                  [:li#logo
                                   [:img {:src "/logo.svg" :alt "Form to Mail logo"}]]
                                  [:li
                                   [:hgroup
                                    [:h1 "Form to Mail"]
                                    [:p "Web forms for static websites"]]]]]]
        intro-section          [:section#intro
                                [:p "Form to Mail is a lightweight web service that sends data submitted from a standard HTML form as an email via any SMTP server. The service verifies sender's email address before delivering the form. It works with standard HTML forms and it's easy to self host."]]
        features-section       [:section#features.grid
                                [:div
                                 [:img.icon {:src "/streamline-emojis--wrench.svg"}]
                                 [:p
                                  "Standard HTML forms"
                                  [:small "without JavaScript or other bullshit"]]]
                                [:div
                                 [:img.icon {:src "/streamline-emojis--open-mailbox-with-raised-flag.svg"}]
                                 [:p
                                  "Verifies sender's address"
                                  [:small "before delivering you a form"]]]
                                [:div
                                 [:img.icon {:src "/streamline-emojis--house-with-garden.svg"}]
                                 [:p
                                  "Delightful to self-host"
                                  [:small "with " [:code "java -jar"] " or a NixOS module"]]]]
        call-to-action-section [:section#call-to-action.grid
                                [:a {:href   "https://github.com/jewiet/form-to-mail/releases"
                                     :target "_blank"
                                     :role   "button"}
                                 "Download"]
                                [:a {:href   "https://github.com/jewiet/form-to-mail/"
                                     :target "_blank"
                                     :role   "button"
                                     :class  "outline"}
                                 "More info"]]
        contact-section        [:section#contact
                                [:h3 "Get in touch"]
                                [:form {:method "POST" :action "https://use.formtomail.eu/submit/1234"}
                                 [:label {:for "email"} "Your email address"]
                                 [:input {:type "email" :name "email" :id "email" :placeholder "me@example.com" :required true}]
                                 [:label {:for "message"} "Your message"]
                                 [:textarea {:name "message" :id "message" :placeholder "If you write something nice I might respond to you..."}]
                                 [:button {:type "submit"} "Send"]]]
        html-main              [:main {:class "container"}
                                intro-section
                                features-section
                                call-to-action-section
                                contact-section]
        html-body              [:body
                                html-header
                                html-main]]
    (str (hiccup/raw "<!doctype html>")
         (hiccup/html [:html {:lang "en"}
                       html-head
                       html-body]))))

(def thank-you-html
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
                                 "Thank you for submitting the form. We will geet back to you as soon as possible."]]

        html-body              [:body
                                html-header
                                html-main]]
    (str (hiccup/raw "<!doctype html>")
         (hiccup/html [:html {:lang "en" :class "thank-you app"}
                       html-head
                       html-body]))))

(def sample-form
  (str (hiccup/raw "<!doctype html>")
       (hiccup/html {:lang "en" :class "samples"}
                    [:html
                     [:head
                      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                      [:link {:type "text/css", :href "/pico.min.css", :rel "stylesheet"}]]

                     [:body
                      [:main {:class "container"}
                       [:form {:method "POST", :action "http://localhost:8080/submit/2345"}
                        [:label {:for "address"} "Street and house number"]
                        [:input {:type "text", :name "address", :id "address"}]
                        [:label {:for "city"} "City"]
                        [:input {:type "text", :name "city", :id "city"}]
                        [:label {:for "country"} "Country"]
                        [:input {:type "text", :name "country", :id "country"}]
                        [:label {:for "email"} "Email"]
                        [:input {:type "email", :name "email", :id "email"}]
                        [:fieldset
                         [:legend "Delivery method"]
                         [:input {:type "radio", :name "delivery-method", :id "home-delivery", :value "home-delivery", :checked true}]
                         [:label {:for "home-delivery"} "Home Delivery"]
                         [:input {:type "radio", :name "delivery-method", :id "pickup", :value "pickup"}]
                         [:label {:for "pickup"} "Pickup"]]
                        [:label {:for "wood-type"} "Select type of wood"]
                        [:select {:name "wood-type", :id "wood-type"}
                         [:option {:value "oak"} "Oak"]
                         [:option {:value "birch"} "Birch"]
                         [:option {:value "pine"} "Pine"]]
                        [:label {:for "coating"} "Select coating"]
                        [:select {:name "coating", :id "coating"}
                         [:option {:value "clear"} "Clear"]
                         [:option {:value "red"} "Red"]
                         [:option {:value "black"} "Black"]]
                        [:label {:for "chairs"} "Number of chairs"]
                        [:input {:type "number", :name "chairs", :id "chairs"}]
                        [:label {:for "high-chairs"} "Number of high chairs"]
                        [:input {:type "number", :name "high-chairs", :id "high-chairs"}]
                        [:label {:for "notes"} "Notes"]
                        [:textarea {:name "notes", :id "notes"}]
                        [:button {:type "submit"} "Send"]]]]])))

(def website
  "A map representing the directory to create and populate.

   The keys must be relative paths represented as strings. The values will be
   treated differently, depending on their types:

   A string will be written verbatim into a file.

   A Path (as returned by babashka.fs/path) will be copied (recursively if it points to a directory).

   A map will be populated recursively."
  {"index.html" index-html
   "thank-you.html" thank-you-html
   "." (fs/path "resources/public/")})

