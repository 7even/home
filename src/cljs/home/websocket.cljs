(ns home.websocket
  (:require [re-frame.core :as rf]
            [wscljs.client :as ws]
            [cljs.reader :refer [read-string]]))

(defn- socket-url []
  (let [page-protocol (.. js/window -location -protocol)
        socket-protocol (case page-protocol
                          "http:" "ws:"
                          "https:" "wss:")
        host (.. js/window -location -host)]
    (str socket-protocol "//" host "/api/ws")))

(defonce connection (atom nil))

(defn handle-server-message [{state :state/data
                              events :events
                              :as message}]
  (cond
    (some? state) (rf/dispatch [:home.events/state-loaded state])
    (some? events) (doseq [{:event/keys [name data]} events]
                     (case name
                       :rss/created (rf/dispatch [:home.events/rss-created data])
                       :rss/updated (rf/dispatch [:home.events/rss-updated data])
                       :rss/deleted (rf/dispatch [:home.events/rss-deleted data])
                       (println "Server sent an unknown event:" name)))
    :else (println "Server sent an unexpected message:" message)))

(defn- connect []
  (ws/create (socket-url)
             {:on-open #(println "Connected")
              :on-close #(println "Disconnected")
              :on-message (fn [e]
                            (let [data (read-string (.-data e))]
                              (handle-server-message data)))}))

(rf/reg-fx :initialize-ws
           (fn [_]
             (println "Initializing WS connection...")
             (reset! connection (connect))))

(rf/reg-fx :terminate-ws
           (fn [_]
             (println "Terminating WS connection...")
             (ws/close @connection)
             (reset! connection nil)))
