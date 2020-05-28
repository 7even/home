(ns home.rss
  (:require [home.db.rss :as db.rss]
            [home.rss.parser :refer [xml->news]]))

(defn load-news [url]
  (-> url slurp xml->news))

(defn- assoc-news [rss]
  (assoc rss :rss/news (-> rss :rss/url load-news)))

(defn get-news [db]
  (->> (db.rss/list-rss db)
       (mapv assoc-news)))

(defn serialize [db id]
  (let [feed (db.rss/get-rss db id)]
    (assoc-news feed)))
