(ns home.rss
  (:require [clojure.data.xml :refer [parse-str]]
            [clojure.string :as str])
  (:import java.text.SimpleDateFormat))

(defn find-tag [tag nodes]
  (->> nodes
       (filter #(= (:tag %) tag))
       first))

(defn- get-content [node]
  (->> (:content node)
       (filter map?)))

(defn tag-content [tag nodes]
  (-> (find-tag tag nodes)
      :content
      first
      str/trim))

(defn parse-date [date-str]
  (.parse (SimpleDateFormat. "E, dd MMM yyyy HH:mm:ss XX" java.util.Locale/US)
          date-str))

(defn get-news [rss-url]
  (->> (slurp rss-url)
       parse-str
       get-content
       first
       get-content
       (filter #(= (:tag %) :item))
       (map (fn [{:keys [content]}]
              {:title (tag-content :title content)
               :link (tag-content :link content)
               :image-url (-> (find-tag :enclosure content)
                              (get-in [:attrs :url]))
               :description (tag-content :description content)
               :published-at (parse-date (tag-content :pubDate content))}))))
