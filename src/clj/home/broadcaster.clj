(ns home.broadcaster
  (:require [clojure.core.async :refer [thread]]
            [home.db :as db]
            [manifold.bus :as bus]))

(defn- broadcast [queue bus enabled?]
  (thread
    (while @enabled?
      (let [report (.take queue)
            events (db/format-changes report)]
        (bus/publish! bus :events (pr-str {:events/data events}))))))

(defn start [db-conn]
  (let [queue (db/get-queue db-conn)
        bus (bus/event-bus)
        enabled? (atom true)]
    (broadcast queue bus enabled?)
    {:enabled? enabled?
     :bus bus
     :db-conn db-conn}))

(defn stop [{:keys [enabled? db-conn]}]
  (reset! enabled? false)
  (db/remove-queue db-conn))
