(ns home.websocket-test
  (:require [clojure.test :refer :all]
            [home.rss :as rss]
            [home.test :refer :all]
            [home.websocket :as ws]
            [home.websocket.utils :refer [take-from-ws]]))

(defn- ws-config []
  {:ws-conn @ws-conn
   :db-conn @db-conn
   :broadcaster {:bus @bus}})

(use-fixtures :each with-db with-broadcaster with-ws)

(deftest handle-connection-test
  (create-rss-feeds)
  (ws/handle-connection (ws-config))
  (is (= {:state/data {:rss (rss/get-news (db))}}
         (take-from-ws (server->client)))))
