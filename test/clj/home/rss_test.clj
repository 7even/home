(ns home.rss-test
  (:require [clojure.test :refer :all]
            [home.rss :as rss]
            [home.test :refer :all]))

(use-fixtures :each with-db)

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
