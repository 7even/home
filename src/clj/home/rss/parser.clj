(ns home.rss.parser
  (:require [clojure.data.xml :refer [parse-str]]
            [clojure.string :as str])
  (:import java.text.SimpleDateFormat))

(defn- find-tag [tag nodes]
  (->> nodes
       (filter #(= (:tag %) tag))
       first))

(defn- get-content [node]
  (->> (:content node)
       (filter map?)))

(defn- tag-content [tag nodes]
  (some-> (find-tag tag nodes)
          :content
          first
          str/trim))

(def date-format
  (SimpleDateFormat. "E, dd MMM yyyy HH:mm:ss XX" java.util.Locale/US))

(defn- parse-date [date-str]
  (.parse date-format date-str))

(defn xml->news [s]
  (->> (parse-str s)
       get-content
       first
       get-content
       (filter #(= (:tag %) :item))
       (map (fn [{:keys [content]}]
              {:title (tag-content :title content)
               :url (tag-content :link content)
               :image-url (-> (find-tag :enclosure content)
                              (get-in [:attrs :url]))
               :description (tag-content :description content)
               :published-at (parse-date (tag-content :pubDate content))}))
       (mapv (fn [attrs]
               (->> attrs
                    (remove (comp nil? val))
                    (into {}))))))
