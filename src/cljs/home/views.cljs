(ns home.views
  (:require [re-frame.core :as rf]
            [home.subs :as subs]))

(defn interface []
  [:div
   [:h1 "RSS"]
   (for [{:rss/keys [id name news]} @(rf/subscribe [::subs/rss])]
     ^{:key id}
     [:div
      [:h2 name]
      [:ul
       (for [{:keys [title url image-url description published-at]} news]
         ^{:key url}
         [:li [:a {:href url
                   :target :blank}
               title]])]])])
