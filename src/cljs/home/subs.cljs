(ns home.subs
  (:require [re-frame.core :as rf]
            home.spec
            [cljs.spec.alpha :as s]))

(rf/reg-sub ::rss-feeds
            (fn [db]
              (get-in db [:rss :local])))

(rf/reg-sub ::rss-name-invalid?
            (fn [db [_ rss-idx]]
              (let [rss-name (get-in db [:rss :local rss-idx :rss/name])]
                (not (s/valid? :rss/name rss-name)))))

(rf/reg-sub ::rss-url-invalid?
            (fn [db [_ rss-idx]]
              (let [rss-url (get-in db [:rss :local rss-idx :rss/url])]
                (not (s/valid? :rss/url rss-url)))))

(rf/reg-sub ::rss-form-invalid?
            (fn [db]
              (some (fn [{:rss/keys [name url]}]
                      (or (not (s/valid? :rss/name name))
                          (not (s/valid? :rss/url url))))
                    (get-in db [:rss :local]))))

(rf/reg-sub ::rss-sync-in-progress?
            (fn [db]
              (get-in db [:rss :sync-in-progress?])))

(rf/reg-sub ::rss-submit-disabled?
            (fn []
              [(rf/subscribe [::rss-form-invalid?])
               (rf/subscribe [::rss-sync-in-progress?])])
            (fn [[form-invalid? sync-in-progress?]]
              (or form-invalid? sync-in-progress?)))

(rf/reg-sub ::rss-items
            (fn [db]
              (->> (get-in db [:rss :remote])
                   (mapcat (fn [rss]
                             (map #(assoc % :source (:rss/name rss))
                                  (:rss/news rss))))
                   (sort-by :published-at >)
                   (take 30))))
