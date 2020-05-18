(ns home.core
  (:require [reagent.dom :as rd]
            [re-frame.core :as rf]
            [home.events :as events]
            [home.views :as views]))

(defn render []
  (rd/render [views/interface]
             (js/document.getElementById "root")))

(defn reload []
  (rf/clear-subscription-cache!)
  (render))

(defn init []
  (rf/dispatch-sync [::events/initialize])
  (render))
