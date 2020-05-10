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
