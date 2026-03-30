(ns www.core
  (:require
   [babashka.fs :as fs]
   [hiccup2.core :as hiccup]))

(def index-html
  (let [html-head              [:head
                                [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                                [:link {:type "text/css", :href "/resources/pico.min.css", :rel "stylesheet"}]
                                [:link {:type "text/css", :href "/resources/style.css", :rel "stylesheet"}]]
        html-header            [:header.container
                                [:hgroup
                                 [:h1 "Form to Mail"]
                                 [:p "Web forms for static websites"]]]
        intro-section          [:section#intro
                                [:p "Form to Mail is a lightweight web service that sends data submitted from a standard HTML form as an email via any SMTP server. The service verifies sender's email address before delivering the form. It works with standard HTML forms and it's easy to self host."]]
        features-section       [:section#features.grid
                                [:div
                                 [:img.icon {:src "/resources/streamline-emojis--wrench.svg"}]
                                 [:p
                                  "Standard HTML forms"
                                  [:small "without JavaScript or other bullshit"]]]
                                [:div
                                 [:img.icon {:src "/resources/streamline-emojis--open-mailbox-with-raised-flag.svg"}]
                                 [:p
                                  "Verifies sender's address"
                                  [:small "before delivering you a form"]]]
                                [:div
                                 [:img.icon {:src "/resources/streamline-emojis--house-with-garden.svg"}]
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
                                 [:label {:for "email"} "Email"]
                                 [:input {:type "email" :name "email" :id "email" :placeholder "Email" :required true}]
                                 [:label {:for "message"} "Your message"]
                                 [:textarea {:name "message" :id "message" :placeholder "Your messsage"}]
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

(def website
  "A map representing the directory to create and populate.

   The keys must be relative paths represented as strings. The values will be
   treated differently, depending on their types:

   A string will be written verbatim into a file.

   A Path (as returned by babashka.fs/path) will be copied (recursively if it points to a directory).

   A map will be populated recursively."
  {"index.html" index-html
   "resources" (fs/path "resources/public/")})
