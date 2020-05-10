(ns home.db.rss
  (:require [datomic.api :as d]))

(defn list-rss [db]
  (->> (d/q '[:find [?rss ...]
              :where [?rss :rss/id]]
            db)
       (map (fn [rss-id]
              (let [rss (d/entity db rss-id)]
                (select-keys rss [:rss/id :rss/name :rss/url]))))))
