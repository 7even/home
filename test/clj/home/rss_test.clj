(ns home.rss-test
  (:require [clojure.java.io :refer [resource]]
            [clojure.test :refer :all]
            [home.rss :as rss]
            [home.test :refer :all]))

(use-fixtures :each with-db)

(deftest get-news-test
  (let [ved-url (-> "files/vedomosti.xml"
                    resource
                    str)
        med-url (-> "files/meduza.xml"
                    resource
                    str)
        [ved-id med-id] (create-rss-feeds ved-url med-url)
        news (rss/get-news (db))]
    (is (= 2 (count news)))
    (let [[vedomosti meduza] news]
      (is (= [:rss/id :rss/name :rss/news] (keys vedomosti)))
      (is (= ved-id (:rss/id vedomosti)))
      (is (= "Vedomosti" (:rss/name vedomosti)))
      (is (= 3 (-> vedomosti :rss/news count)))
      (is (= [:rss/id :rss/name :rss/news] (keys meduza)))
      (is (= med-id (:rss/id meduza)))
      (is (= "Meduza" (:rss/name meduza)))
      (is (= 3 (-> meduza :rss/news count))))))
