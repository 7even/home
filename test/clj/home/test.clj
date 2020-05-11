(ns home.test
  (:require [datomic.api :as d]
            [home.core :as core]
            [integrant.core :as ig]))

(def config
  (core/config :test))

(def db-config
  (:datomic/client config))

(def db-conn
  (atom nil))

(defn db []
  (d/db @db-conn))

(defn with-db [tests]
  (reset! db-conn
          (ig/init-key :datomic/client db-config))
  (tests)
  (d/delete-database (:uri db-config))
  (reset! db-conn nil))

(defn create-rss-feeds []
  (let [ved-id (d/squuid)
        med-id (d/squuid)]
    (d/transact @db-conn
                [{:rss/id ved-id
                  :rss/name "Vedomosti"
                  :rss/url "https://www.vedomosti.ru/rss/news"}
                 {:rss/id med-id
                  :rss/name "Meduza"
                  :rss/url "https://meduza.io/rss/news"}])
    [ved-id med-id]))
