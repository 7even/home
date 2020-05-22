(ns home.rss-test
  (:require [clojure.test :refer :all]
            [home.rss :as rss]
            [home.test :refer :all]))

(use-fixtures :each with-db)

(deftest get-news-test
  (let [[ved-id med-id] (create-rss-feeds)
        news (rss/get-news (db))]
    (is (= 2 (count news)))
    (let [[vedomosti meduza] news]
      (is (= [:rss/id :rss/name :rss/url :rss/news] (keys vedomosti)))
      (is (= ved-id (:rss/id vedomosti)))
      (is (= "Vedomosti" (:rss/name vedomosti)))
      (is (= 3 (-> vedomosti :rss/news count)))
      (is (= [:rss/id :rss/name :rss/url :rss/news] (keys meduza)))
      (is (= med-id (:rss/id meduza)))
      (is (= "Meduza" (:rss/name meduza)))
      (is (= 3 (-> meduza :rss/news count))))))
