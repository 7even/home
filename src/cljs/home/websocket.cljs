(ns home.websocket
  (:require [re-frame.core :as rf]
            [wscljs.client :as ws]
            [wscljs.format :as fmt]
            [cljs.reader :refer [read-string]]
            [cljs.spec.alpha :as s]))

(defn- socket-url []
  (let [page-protocol (.. js/window -location -protocol)
        socket-protocol (case page-protocol
                          "http:" "ws:"
                          "https:" "wss:")
        host (.. js/window -location -host)]
    (str socket-protocol "//" host "/api/ws")))

(defonce connection (atom nil))

(defonce commands (atom {}))

(defn- handle-event [{:event/keys [name data]
                      command-id :command/id}]
  (case name
    :rss/created (rf/dispatch [:home.events/rss-created data])
    :rss/updated (rf/dispatch [:home.events/rss-updated data])
    :rss/deleted (rf/dispatch [:home.events/rss-deleted data])
    (println "Server sent an unknown event:" name))
  (when-let [{:keys [success]} (get @commands command-id)]
    (rf/dispatch [success])
    (swap! commands dissoc command-id)))

(s/def :state/data map?)
(s/def :events/data vector?)

(s/def ::state
  (s/keys :req [:state/data]))

(s/def ::events
  (s/keys :req [:events/data]))

(s/def ::server-message
  (s/or :state ::state
        :events ::events))

(defn handle-server-message [raw-message]
  (let [message (s/conform ::server-message raw-message)]
    (if (= message ::s/invalid)
      (println "Server sent an unexpected message:" message)
      (let [[type payload] message]
        (case type
          :state (rf/dispatch [:home.events/state-loaded (:state/data payload)])
          :events (doseq [event (:events/data payload)]
                    (handle-event event)))))))

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

(rf/reg-fx :send-to-ws
           (fn [payload]
             (ws/send @connection payload fmt/edn)))

(rf/reg-fx :enqueue-command
           (fn [[command-id on-success on-error]]
             (swap! commands
                    assoc
                    command-id
                    {:success on-success
                     :error on-error})))
