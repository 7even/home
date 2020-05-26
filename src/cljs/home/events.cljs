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

(rf/reg-event-db ::rss-created
                 (fn [db [_ {:rss/keys [id] :as rss-attrs}]]
                   (cond-> db
                     true
                     (update-in [:remote :rss] conj rss-attrs)
                     (seq (get-in db [:local :rss]))
                     (assoc-in [:local :rss id]
                               (select-keys rss-attrs [:rss/name :rss/url])))))

(rf/reg-event-db ::rss-updated
                 (fn [db [_ {:rss/keys [id] :as new-rss-attrs}]]
                   (cond-> db
                     true
                     (update-in [:remote :rss]
                                (fn [feeds]
                                  (map (fn [feed]
                                         (if (= (:rss/id feed) id)
                                           (merge feed new-rss-attrs)
                                           feed))
                                       feeds)))
                     (seq (get-in db [:local :rss]))
                     (update-in [:local :rss id]
                                merge
                                (dissoc new-rss-attrs :rss/id)))))

(rf/reg-event-db ::rss-deleted
                 (fn [db [_ {:rss/keys [id]}]]
                   (cond-> db
                     true
                     (update-in [:remote :rss]
                                (fn [feeds]
                                  (remove #(= (:rss/id %) id)
                                          feeds)))
                     (seq (get-in db [:local :rss]))
                     (update-in [:local :rss] dissoc id))))

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
