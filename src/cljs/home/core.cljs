(ns home.core
  (:require [reagent.dom :as rd]
            [re-frame.core :as rf]
            [home.events :as events]
            [home.views :as views]))

(defn render []
  (rd/render [views/interface]
             (js/document.getElementById "root")))

(defn init []
  (rf/dispatch-sync [::events/initialize])
  (render))
