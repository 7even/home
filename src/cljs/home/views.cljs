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
   [:div.col-7
    [:input.form-control {:type "text"
                          :value url
                          :class (when @(rf/subscribe [::subs/rss-url-invalid? idx])
                                   :is-invalid)
                          :on-change #(rf/dispatch [::events/change-rss-url
                                                    idx
                                                    (-> % .-target .-value)])}]]
   [:div.col-1
    [:button.close {:type "button"
                    :style {:margin-top "5px"}
                    :on-click #(rf/dispatch [::events/delete-rss idx])}
     [:span "×"]]]])

(defn rss-manager []
  [:div
   [:div.modal-header
    [:h5.modal-title "RSS feeds"]
    [:button.close {:type "button"
                    :data-dismiss "modal"}
     [:span "×"]]]
   [:div.modal-body.pb-0
    (let [feeds @(rf/subscribe [::subs/rss-feeds])]
      (if (seq feeds)
        [:form
         [:fieldset {:disabled @(rf/subscribe [::subs/rss-sync-in-progress?])}
          (map-indexed (fn [idx rss-attrs]
                         ^{:key idx} [rss-feed idx rss-attrs])
                       feeds)]]
        [:p>em "No feeds yet"]))
    (when-let [server-error @(rf/subscribe [::subs/rss-server-error])]
      [:div.alert.alert-danger.px-2.py-1 server-error])]
   [:div.modal-footer
    [:button.btn.btn-secondary
     {:on-click #(rf/dispatch [::events/add-rss])
      :disabled @(rf/subscribe [::subs/rss-sync-in-progress?])}
     "New RSS feed"]
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
  (if @(rf/subscribe [::subs/state-loaded?])
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
          ^{:key (:url item)} [news-item item])]]]]
    [:div.vh-100.d-flex.justify-content-center.align-items-center
     (for [_ (range 0 3)]
       [:div.spinner-grow.m-3])]))
