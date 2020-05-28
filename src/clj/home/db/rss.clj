(ns home.db.rss
  (:require [datomic.api :as d]))

(defn create-rss-tx [attrs]
  (assoc attrs :rss/id (d/squuid)))

(defn delete-rss-tx [id]
  [:db/retractEntity [:rss/id id]])

(defn list-rss [db]
  (->> (d/q '[:find [?rss ...]
              :where [?rss :rss/id]]
            db)
       (map (fn [rss-id]
              (let [rss (d/entity db rss-id)]
                (select-keys rss [:rss/id :rss/name :rss/url]))))))

(defn get-rss [db id]
  (d/pull db
          [:rss/id :rss/name :rss/url]
          [:rss/id id]))
