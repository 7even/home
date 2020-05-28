(ns home.db.rss-test
  (:require [clojure.test :refer :all]
            [home.db.rss :as db.rss]
            [home.test :refer :all]))

(use-fixtures :each with-db)

(deftest list-rss-test
  (let [[ved-id med-id] (create-rss-feeds "https://www.vedomosti.ru/rss/news"
                                          "https://meduza.io/rss/news")
        feeds (db.rss/list-rss (db))]
    (is (= #{{:rss/id ved-id
              :rss/name "Vedomosti"
              :rss/url "https://www.vedomosti.ru/rss/news"}
             {:rss/id med-id
              :rss/name "Meduza"
              :rss/url "https://meduza.io/rss/news"}}
           (set feeds)))))
