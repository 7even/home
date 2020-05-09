(ns user
  (:require [home.core :refer [config]]
            [integrant.repl :refer [go halt]]))

(integrant.repl/set-prep! #(config :development))

(defn system []
  integrant.repl.state/system)

(defn db-conn []
  (:datomic/client (system)))

(defn db []
  (datomic.api/db (db-conn)))

(def reset integrant.repl/reset)
