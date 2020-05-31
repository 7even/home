(ns home.views
  (:require [re-frame.core :as rf]
            [cljs-time.core :as t]
            [cljs-time.format :as f]
            [reagent-modals.modals :as modals]
            [home.events :as events]
            [home.subs :as subs]))

(defn- format-date [date]
  (let [formatter (f/formatter "HH:mm")]
    (f/unparse formatter (t/to-default-time-zone date))))

(defn rss-feed [idx {:rss/keys [id name url]}]
  [:div.form-row.form-group
   [:div.col-4
    [:input.form-control {:type "text"
                          :value name
                          :class (when @(rf/subscribe [::subs/rss-name-invalid? idx])
                                   :is-invalid)
                          :on-change #(rf/dispatch [::events/change-rss-name
                                                    idx
                                                    (-> % .-target .-value)])}]]
   [:div.col-8
    [:input.form-control {:type "text"
                          :value url
                          :class (when @(rf/subscribe [::subs/rss-url-invalid? idx])
                                   :is-invalid)
                          :on-change #(rf/dispatch [::events/change-rss-url
                                                    idx
                                                    (-> % .-target .-value)])}]]])

(defn rss-manager []
  [:div
   [:div.modal-header
    [:h5.modal-title "RSS feeds"]
    [:button.close {:type "button"
                    :data-dismiss "modal"}
     [:span "×"]]]
   [:div.modal-body.pb-0
    [:form
     [:fieldset {:disabled @(rf/subscribe [::subs/rss-sync-in-progress?])}
      (map-indexed (fn [idx rss-attrs]
                     ^{:key idx} [rss-feed idx rss-attrs])
                   @(rf/subscribe [::subs/rss-feeds]))]]]
   [:div.modal-footer
    [:button.btn.btn-primary
     {:on-click #(rf/dispatch [::events/synchronize-rss])
      :disabled @(rf/subscribe [::subs/rss-submit-disabled?])}
     "Save"]]])

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
        {:on-click (fn []
                     (rf/dispatch-sync [::events/begin-editing-rss])
                     (modals/modal! [rss-manager]
                                    {:hidden #(rf/dispatch [::events/stop-editing-rss])}))}
        "Manage RSS feeds"]]
      (for [item @(rf/subscribe [::subs/rss-items])]
        ^{:key (:url item)} [news-item item])]]]])
