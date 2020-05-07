(ns home.core
  (:require [reagent.dom :as rd]
            [re-frame.core :as rf]))

(defn hello []
  [:h1 "Hello from re-frame"])

(defn render []
  (rd/render [hello]
             (js/document.getElementById "root")))

(defn init []
  (println "Hello!")
  (render))
