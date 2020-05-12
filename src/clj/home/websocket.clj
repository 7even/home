(ns home.websocket
  (:require [datomic.api :as d]
            [home.rss :as rss]
            [home.websocket.utils :refer [send-to-ws]]))

(defn get-initial-state [db]
  {:state/data {:rss (rss/get-news db)}})

(defn handle-connection [{:keys [ws-conn db-conn]}]
  (send-to-ws ws-conn (get-initial-state (d/db db-conn))))
