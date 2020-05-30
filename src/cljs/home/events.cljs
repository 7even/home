(ns home.events
  (:require [re-frame.core :as rf]
            home.websocket))

(rf/reg-event-fx ::initialize
                 (fn []
                   {:db {:commands {}
                         :rss {:remote []
                               :local {}
                               :sync-in-progress? false}}
                    :initialize-ws nil}))

(rf/reg-event-db ::state-loaded
                 (fn [db [_ state]]
                   (assoc-in db [:rss :remote] (:rss state))))

(rf/reg-event-db ::rss-created
                 (fn [db [_ {:rss/keys [id] :as rss-attrs}]]
                   (cond-> db
                     true
                     (update-in [:rss :remote] conj rss-attrs)
                     (seq (get-in db [:rss :local]))
                     (assoc-in [:rss :local id]
                               (select-keys rss-attrs [:rss/name :rss/url])))))

(rf/reg-event-db ::rss-updated
                 (fn [db [_ {:rss/keys [id] :as new-rss-attrs}]]
                   (cond-> db
                     true
                     (update-in [:rss :remote]
                                (fn [feeds]
                                  (map (fn [feed]
                                         (if (= (:rss/id feed) id)
                                           (merge feed new-rss-attrs)
                                           feed))
                                       feeds)))
                     (seq (get-in db [:rss :local]))
                     (update-in [:rss :local id]
                                merge
                                (dissoc new-rss-attrs :rss/id)))))

(rf/reg-event-db ::rss-deleted
                 (fn [db [_ {:rss/keys [id]}]]
                   (cond-> db
                     true
                     (update-in [:rss :remote]
                                (fn [feeds]
                                  (remove #(= (:rss/id %) id)
                                          feeds)))
                     (seq (get-in db [:rss :local]))
                     (update-in [:rss :local] dissoc id))))

(rf/reg-event-db ::begin-editing-rss
                 (fn [db]
                   (assoc-in db
                             [:rss :local]
                             (->> (get-in db [:rss :remote])
                                  (reduce (fn [feeds feed]
                                            (assoc feeds
                                                   (:rss/id feed)
                                                   (select-keys feed [:rss/name :rss/url])))
                                          {})
                                  (into (array-map))))))

(rf/reg-event-db ::change-rss-name
                 (fn [db [_ id new-name]]
                   (assoc-in db
                             [:rss :local id :rss/name]
                             new-name)))

(rf/reg-event-db ::change-rss-url
                 (fn [db [_ id new-url]]
                   (assoc-in db
                             [:rss :local id :rss/url]
                             new-url)))

(rf/reg-event-fx ::synchronize-rss
                 (fn [{:keys [db]}]
                   (let [command-id (random-uuid)
                         feeds (mapv (fn [[id attrs]]
                                       (merge {:rss/id id} attrs))
                                     (get-in db [:rss :local]))]
                     {:db (assoc-in db [:rss :sync-in-progress?] true)
                      :enqueue-command [command-id ::rss-synchronized :rss-failed-to-synchronize]
                      :send-to-ws {:command/id command-id
                                   :command/name :rss/synchronize
                                   :command/data feeds}})))

(rf/reg-event-db ::rss-synchronized
                 (fn [db]
                   (assoc-in db [:rss :sync-in-progress?] false)))

(rf/reg-event-db ::stop-editing-rss
                 (fn [db]
                   (assoc-in db [:rss :local] (array-map))))
