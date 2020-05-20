(ns home.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub ::rss-feeds
            (fn [db]
              (get-in db [:local :rss])))

(rf/reg-sub ::rss-items
            (fn [db]
              (->> (get-in db [:remote :rss])
                   (mapcat (fn [rss]
                             (map #(assoc % :source (:rss/name rss))
                                  (:rss/news rss))))
                   (sort-by :published-at >)
                   (take 30))))
