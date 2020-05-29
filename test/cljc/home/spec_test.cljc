(ns home.spec-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]))

(require 'home.spec)

(deftest rss-id-test
  (is (s/valid? :rss/id (java.util.UUID/randomUUID)))
  (is (not (s/valid? :rss/id "foobar"))))

(deftest rss-name-test
  (is (s/valid? :rss/name "Vedomosti"))
  (is (not (s/valid? :rss/name nil)))
  (is (not (s/valid? :rss/name 123))))

(deftest rss-url-test
  (is (s/valid? :rss/url "https://vedomosti.ru/rss/news"))
  (is (not (s/valid? :rss/url "")))
  (is (not (s/valid? :rss/url nil))))
