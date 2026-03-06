(ns www.core
  (:require
   [hiccup2.core :as hiccup]))

(defn build []
  (print (str (hiccup/raw "<!doctype html>")
              (hiccup/html [:html {:lang "en"}
                            [:head]
                            [:body]]))))
