(ns home.test
  (:require [clojure.java.io :refer [resource]]
            [datomic.api :as d]
            [home.broadcaster :as broadcaster]
            [home.core :as core]
            [integrant.core :as ig]
            [manifold.stream :as s]))

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

(def bus
  (atom nil))

(defn with-broadcaster [tests]
  (let [{broadcaster-bus :bus
         :as broadcaster} (broadcaster/start @db-conn)]
    (reset! bus broadcaster-bus)
    (tests)
    (broadcaster/stop broadcaster)))

(def ws-conn
  (atom nil))

(defn server->client []
  (.sink @ws-conn))

(defn client->server []
  (.source @ws-conn))

(defn with-ws [tests]
  (reset! ws-conn
          (s/splice (s/stream)
                    (s/stream)))
  (tests)
  (s/close! @ws-conn)
  (reset! ws-conn nil))

(defn local-file-url [filename]
  (-> (str "files/" filename)
      resource
      str))

(defn create-rss
  ([] (create-rss {}))
  ([rss-attrs]
   (let [attrs (merge {:rss/id (d/squuid)
                       :rss/name "Vedomosti"
                       :rss/url (local-file-url "vedomosti.xml")}
                      rss-attrs)]
     (d/transact @db-conn [attrs])
     (:rss/id attrs))))

(defn create-rss-feeds
  ([]
   (let [ved-url (local-file-url "vedomosti.xml")
         med-url (local-file-url "meduza.xml")]
     (create-rss-feeds ved-url med-url)))
  ([ved-url med-url]
   (mapv create-rss
         [{:rss/name "Vedomosti"
           :rss/url ved-url}
          {:rss/name "Meduza"
           :rss/url med-url}])))
