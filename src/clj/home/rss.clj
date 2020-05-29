(ns home.rss
  (:require [clojure.set :as set]
            [datomic.api :as d]
            [home.db.rss :as db.rss]
            [home.rss.parser :refer [xml->news]]))

(defn synchronize-rss [db-conn new-rss-list command-id]
  (let [old-ids (->> (db.rss/list-rss (d/db db-conn))
                     (map :rss/id)
                     set)
        new-ids (->> new-rss-list
                     (map :rss/id)
                     (keep identity)
                     set)
        created-feeds (remove #(contains? % :rss/id)
                              new-rss-list)
        updated-feeds (filter #(old-ids (:rss/id %))
                              new-rss-list)
        deleted-feed-ids (set/difference old-ids new-ids)
        tx (concat (map db.rss/create-rss-tx created-feeds)
                   updated-feeds
                   (map db.rss/delete-rss-tx deleted-feed-ids)
                   [{:db/id "datomic.tx"
                     :command/id command-id}])]
    @(d/transact db-conn tx)))

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
