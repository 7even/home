(ns home.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub ::rss-items
            (fn [db]
              (->> (:rss db)
                   (mapcat (fn [rss]
                             (map #(assoc % :source (:rss/name rss))
                                  (:rss/news rss))))
                   (sort-by :published-at >)
                   (take 30))))
