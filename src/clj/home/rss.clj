(ns home.rss
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [datomic.api :as d]
            [home.db.rss :as db.rss]
            [home.rss.parser :refer [xml->news]]
            [home.websocket.utils :refer [send-to-ws]]))

(require 'home.spec)

(s/def ::synchronize
  (s/coll-of (s/keys :req [:rss/name :rss/url]
                     :opt [:rss/id])
             :kind vector?))

(defn load-news [url]
  (-> url slurp xml->news))

(defn- url-invalid? [{:rss/keys [url]}]
  (try
    (load-news url)
    false
    (catch Exception e
      true)))

(defn synchronize-rss [{:keys [db-conn ws-conn]} new-rss-list command-id]
  (cond
    (not (s/valid? ::synchronize new-rss-list))
    (send-to-ws ws-conn
                {:command/id command-id
                 :command/error (s/explain-str ::synchronize new-rss-list)})
    (not (->> new-rss-list
              (map :rss/url)
              (apply distinct?)))
    (send-to-ws ws-conn
                {:command/id command-id
                 :command/error "Some urls are not unique."})
    (some url-invalid? new-rss-list)
    (send-to-ws ws-conn
                {:command/id command-id
                 :command/error "This is not an RSS url."})
    :else
    (let [old-ids (->> (db.rss/list-rss (d/db db-conn))
                       (map :rss/id)
                       set)
          new-ids (->> new-rss-list
                       (map :rss/id)
                       (keep identity)
                       set)
          created-feeds (->> new-rss-list
                             (remove #(contains? % :rss/id))
                             (map #(select-keys % [:rss/name :rss/url])))
          updated-feeds (->> new-rss-list
                             (filter #(old-ids (:rss/id %)))
                             (map #(select-keys % [:rss/id :rss/name :rss/url])))
          deleted-feed-ids (set/difference old-ids new-ids)
          tx (concat (map db.rss/create-rss-tx created-feeds)
                     updated-feeds
                     (map db.rss/delete-rss-tx deleted-feed-ids)
                     [{:db/id "datomic.tx"
                       :command/id command-id}])]
      @(d/transact db-conn tx))))

(defn- assoc-news [rss]
  (assoc rss :rss/news (-> rss :rss/url load-news)))

(defn get-news [db]
  (->> (db.rss/list-rss db)
       (mapv assoc-news)))

(defn serialize [db id]
  (let [feed (db.rss/get-rss db id)]
    (assoc-news feed)))
