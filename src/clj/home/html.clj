(ns home.html
  (:require [hiccup.page :refer [html5]]))

(def page
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:title "Home dashboard"]
    [:link {:rel "stylesheet"
            :href "https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/css/bootstrap.min.css"
            :integrity "sha384-9aIt2nRpC12Uk9gS9baDl411NQApFmC26EwAOH8WgZl5MYYxFfc+NcPb1dKGj7Sk"
            :crossorigin "anonymous"}]]
   [:body
    [:div#root]
    [:script {:src "/js/main.js"}]]))
