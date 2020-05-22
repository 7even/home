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

(deftest get-rss-test
  (let [[ved-id] (create-rss-feeds)
        feed (rss/serialize (db) ved-id)]
    (is (= [:rss/id :rss/name :rss/url :rss/news]
           (keys feed)))
    (is (= ved-id (:rss/id feed)))
    (is (= "Vedomosti" (:rss/name feed)))
    (is (= 3 (-> feed :rss/news count)))))
