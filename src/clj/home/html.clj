(ns home.html
  (:require [hiccup.page :refer [html5]]))

(def page
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:title "Home dashboard"]]
   [:body
    [:div#root
     [:h1 "Hello"]]]))
