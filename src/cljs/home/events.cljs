(ns home.events
  (:require [re-frame.core :as rf]
            home.websocket))

(defn- update-feed [feeds id new-attrs]
  (mapv (fn [feed]
          (if (= (:rss/id feed) id)
            (merge feed new-attrs)
            feed))
        feeds))

(defn- remove-feed [feeds id]
  (->> feeds
       (remove #(= (:rss/id %) id))
       vec))

(rf/reg-event-fx ::initialize
                 (fn []
                   {:db {:commands {}
                         :rss {:remote []
                               :local []
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
                     (update-in [:rss :local]
                                conj
                                (select-keys rss-attrs [:rss/id :rss/name :rss/url])))))

(rf/reg-event-db ::rss-updated
                 (fn [db [_ {:rss/keys [id] :as new-rss-attrs}]]
                   (cond-> db
                     true
                     (update-in [:rss :remote]
                                (fn [feeds]
                                  (update-feed feeds id new-rss-attrs)))
                     (seq (get-in db [:rss :local]))
                     (update-in [:rss :local]
                                (fn [feeds]
                                  (update-feed feeds
                                               id
                                               (select-keys new-rss-attrs
                                                            [:rss/id :rss/name :rss/url])))))))

(rf/reg-event-db ::rss-deleted
                 (fn [db [_ {:rss/keys [id]}]]
                   (cond-> db
                     true
                     (update-in [:rss :remote]
                                (fn [feeds]
                                  (remove-feed feeds id)))
                     (seq (get-in db [:rss :local]))
                     (update-in [:rss :local]
                                (fn [feeds]
                                  (remove-feed feeds id))))))

(rf/reg-event-db ::begin-editing-rss
                 (fn [db]
                   (assoc-in db
                             [:rss :local]
                             (mapv #(select-keys % [:rss/id :rss/name :rss/url])
                                   (get-in db [:rss :remote])))))

(rf/reg-event-db ::change-rss-name
                 (fn [db [_ idx new-name]]
                   (assoc-in db [:rss :local idx :rss/name] new-name)))

(rf/reg-event-db ::change-rss-url
                 (fn [db [_ idx new-url]]
                   (assoc-in db [:rss :local idx :rss/url] new-url)))

(rf/reg-event-db ::delete-rss
                 (fn [db [_ idx]]
                   (update-in db
                              [:rss :local]
                              #(->> (concat (subvec % 0 idx)
                                            (subvec % (inc idx) (count %)))
                                    vec))))

(rf/reg-event-fx ::synchronize-rss
                 (fn [{:keys [db]}]
                   (let [command-id (random-uuid)]
                     {:db (assoc-in db [:rss :sync-in-progress?] true)
                      :enqueue-command [command-id ::rss-synchronized :rss-failed-to-synchronize]
                      :send-to-ws {:command/id command-id
                                   :command/name :rss/synchronize
                                   :command/data (get-in db [:rss :local])}})))

(rf/reg-event-db ::rss-synchronized
                 (fn [db]
                   (assoc-in db [:rss :sync-in-progress?] false)))

(rf/reg-event-db ::stop-editing-rss
                 (fn [db]
                   (assoc-in db [:rss :local] [])))
