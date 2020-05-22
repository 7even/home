(ns home.websocket
  (:require [datomic.api :as d]
            [home.rss :as rss]
            [home.websocket.utils :refer [send-to-ws]]
            [manifold.bus :as bus]
            [manifold.stream :as ms]))

(defn get-initial-state [db]
  {:state/data {:rss (rss/get-news db)}})

(defn handle-connection [{:keys [ws-conn db-conn]
                          {:keys [bus]} :broadcaster}]
  (send-to-ws ws-conn (get-initial-state (d/db db-conn)))
  (ms/connect (bus/subscribe bus :events)
              ws-conn))
