(ns home.websocket-test
  (:require [clojure.java.io :refer [resource]]
            [clojure.test :refer :all]
            [home.rss :as rss]
            [home.test :refer :all]
            [home.websocket :as ws]
            [home.websocket.utils :refer [take-from-ws]]))

(defn- ws-config []
  {:ws-conn @ws-conn
   :db-conn @db-conn})

(use-fixtures :each with-db with-ws)

(deftest handle-connection-test
  (let [ved-url (-> "files/vedomosti.xml"
                    resource
                    str)
        med-url (-> "files/meduza.xml"
                    resource
                    str)]
    (create-rss-feeds ved-url med-url)
    (ws/handle-connection (ws-config))
    (is (= {:state/data {:rss (rss/get-news (db))}}
           (take-from-ws (server->client))))))
