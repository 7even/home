(ns home.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::non-empty-string
  (s/and string?
         (complement str/blank?)))

(s/def :rss/id uuid?)
(s/def :rss/name ::non-empty-string)
(s/def :rss/url ::non-empty-string)
