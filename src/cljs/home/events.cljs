(ns home.events
  (:require [re-frame.core :as rf]
            home.websocket))

(rf/reg-event-fx ::initialize
                 (fn []
                   {:db {:remote {:rss []}
                         :local {:rss {}}}
                    :initialize-ws nil}))

(rf/reg-event-db ::state-loaded
                 (fn [db [_ state]]
                   (update db :remote merge state)))

(rf/reg-event-db ::begin-editing-rss
                 (fn [db]
                   (assoc-in db
                             [:local :rss]
                             (->> (get-in db [:remote :rss])
                                  (reduce (fn [feeds feed]
                                            (assoc feeds
                                                   (:rss/id feed)
                                                   (select-keys feed [:rss/name :rss/url])))
                                          {})
                                  (into (array-map))))))

(rf/reg-event-db ::change-rss-name
                 (fn [db [_ id new-name]]
                   (assoc-in db
                             [:local :rss id :rss/name]
                             new-name)))

(rf/reg-event-db ::change-rss-url
                 (fn [db [_ id new-url]]
                   (assoc-in db
                             [:local :rss id :rss/url]
                             new-url)))

(rf/reg-event-db ::stop-editing-rss
                 (fn [db]
                   (assoc-in db [:local :rss] (array-map))))
