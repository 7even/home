(ns home.db
  (:require [datomic.api :as d]
            [io.rkn.conformity :as conf]))

(defn setup-db [uri]
  (d/create-database uri)
  (let [conn (d/connect uri)
        norms (conf/read-resource "schema.edn")]
    (conf/ensure-conforms conn norms [:home/schema])
    conn))

(comment
  (d/delete-database (get-in (home.core/config)
                             [:datomic/client :uri])))
