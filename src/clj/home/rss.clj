(ns home.rss
  (:require [home.rss.parser :refer [xml->news]]))

(defn get-news [rss-url]
  (-> rss-url
      slurp
      xml->news))
