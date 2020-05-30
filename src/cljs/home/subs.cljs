(ns home.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub ::rss-feeds
            (fn [db]
              (get-in db [:rss :local])))

(rf/reg-sub ::rss-sync-in-progress?
            (fn [db]
              (get-in db [:rss :sync-in-progress?])))

(rf/reg-sub ::rss-items
            (fn [db]
              (->> (get-in db [:rss :remote])
                   (mapcat (fn [rss]
                             (map #(assoc % :source (:rss/name rss))
                                  (:rss/news rss))))
                   (sort-by :published-at >)
                   (take 30))))
