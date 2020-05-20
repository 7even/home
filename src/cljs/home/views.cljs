(ns home.views
  (:require [re-frame.core :as rf]
            [cljs-time.core :as t]
            [cljs-time.format :as f]
            [reagent-modals.modals :as modals]
            [home.subs :as subs]))

(defn- format-date [date]
  (let [formatter (f/formatter "HH:mm")]
    (f/unparse formatter (t/to-default-time-zone date))))

(defn rss-manager []
  [:div.modal-header
   [:h5.modal-title "RSS feeds"]
   [:button.close {:type "button"
                   :data-dismiss "modal"}
    [:span "×"]]])

(defn news-item [{:keys [title url image-url description published-at source]}]
  [:li.list-group-item
   [:div title]
   [:div.float-right
    [:a {:href url
         :target :blank}
     [:small (str source " @ " (format-date published-at))]]]])

(defn interface []
  [:div.container-fluid
   [modals/modal-window]
   [:div.row
    [:div.col-3
     [:ul.list-group.list-group-flush
      [:li.list-group-item
       [:button.btn.btn-primary
        {:on-click #(modals/modal! [rss-manager])}
        "Manage RSS feeds"]]
      (for [item @(rf/subscribe [::subs/rss-items])]
        ^{:key (:url item)} [news-item item])]]]])
