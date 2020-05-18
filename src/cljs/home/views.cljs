(ns home.views
  (:require [re-frame.core :as rf]
            [cljs-time.core :as t]
            [cljs-time.format :as f]
            [home.subs :as subs]))

(defn- format-date [date]
  (let [formatter (f/formatter "HH:mm dd MMM yyyy")]
    (f/unparse formatter (t/to-default-time-zone date))))

(defn card [{:keys [title url image-url description published-at source]}]
  [:div.card.my-2 {:style {:width "36rem"}}
   [:div.card-body.p-2
    [:p.card-text title]
    [:div.text-right
     [:a {:href url
          :target :blank}
      (str source " @ " (format-date published-at))]]]])

(defn interface []
  [:div
   (for [item @(rf/subscribe [::subs/rss-items])]
     ^{:key (:url item)} [card item])])
