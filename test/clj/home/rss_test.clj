(ns home.rss-test
  (:require [clojure.test :refer :all]
            [home.db.rss :as db.rss]
            [home.rss :as rss]
            [home.test :refer :all]))

(use-fixtures :each with-db)

(deftest synchronize-rss-test
  (let [[ved-id med-id] (create-rss-feeds)]
    (rss/synchronize-rss @db-conn
                         [{:rss/name "Sports.ru"
                           :rss/url "https://www.sports.ru/rss/main.xml"}
                          {:rss/id ved-id
                           :rss/url (local-file-url "meduza.xml")}])
    (let [updated-feeds (db.rss/list-rss (db))
          ids (->> updated-feeds (map :rss/id) set)]
      (is (= 2 (count ids)))
      (is (not (ids med-id)))
      (is (ids ved-id))
      (is (= #{"Sports.ru" "Vedomosti"}
             (->> updated-feeds
                  (map :rss/name)
                  set)))
      (is (= #{"https://www.sports.ru/rss/main.xml"
               (local-file-url "meduza.xml")}
             (->> updated-feeds
                  (map :rss/url)
                  set))))))

(deftest get-news-test
  (let [[ved-id med-id] (create-rss-feeds)
        news (rss/get-news (db))]
    (is (= #{{:rss/id ved-id
              :rss/name "Vedomosti"
              :rss/url (local-file-url "vedomosti.xml")
              :rss/news (-> "vedomosti.xml" local-file-url rss/load-news)}
             {:rss/id med-id
              :rss/name "Meduza"
              :rss/url (local-file-url "meduza.xml")
              :rss/news (-> "meduza.xml" local-file-url rss/load-news)}}
           (set news)))))

(deftest serialize-test
  (let [[ved-id] (create-rss-feeds)
        feed (rss/serialize (db) ved-id)]
    (is (= {:rss/id ved-id
            :rss/name "Vedomosti"
            :rss/url (local-file-url "vedomosti.xml")
            :rss/news (-> "vedomosti.xml" local-file-url rss/load-news)}
           feed))))
