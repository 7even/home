(ns home.rss
  (:require [home.db.rss :as db.rss]
            [home.rss.parser :refer [xml->news]]))

(defn get-news [db]
  (->> (db.rss/list-rss db)
       (mapv (fn [rss]
               (assoc rss
                      :rss/news
                      (-> rss :rss/url slurp xml->news))))))
