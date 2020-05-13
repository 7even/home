(ns home.events
  (:require [re-frame.core :as rf]
            home.websocket))

(rf/reg-event-fx ::initialize
                 (fn []
                   {:db {:rss []}
                    :initialize-ws nil}))

(rf/reg-event-db ::state-loaded
                 (fn [db [_ state]]
                   (merge db state)))
