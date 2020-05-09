(ns home.db
  (:require [datomic.api :as d]))

(defn setup-db [uri]
  (d/create-database uri)
  (let [conn (d/connect uri)]
    conn))

(comment
  (d/delete-database (get-in (home.core/config)
                             [:datomic/client :uri])))
