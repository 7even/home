(ns home.websocket
  (:require [clojure.spec.alpha :as s]
            [datomic.api :as d]
            [home.rss :as rss]
            [home.websocket.utils :refer [send-to-ws]]
            [manifold.bus :as bus]
            [manifold.stream :as ms]))

(def commands
  {:rss/synchronize rss/synchronize-rss})

(s/def :command/id uuid?)
(s/def :command/name (-> commands keys set))
(s/def :command/data (s/or :map map?
                           :vec vector?))

(s/def ::client-message
  (s/keys :req [:command/id :command/name :command/data]))

(defn- parse-client-message [ws-conn message-str]
  (try
    (let [data (read-string message-str)
          message (s/conform ::client-message data)]
      (if (= message ::s/invalid)
        (do
          (send-to-ws ws-conn
                      {:message/error (s/explain-str ::client-message data)})
          nil)
        message))
    (catch RuntimeException e
      (send-to-ws ws-conn
                  {:message/error "Failed to parse the message"})
      nil)))

(defn- handle-client-message [config message-data]
  (let [{:command/keys [id name data]} message-data
        command (get commands name)]
    (command config (second data) id)))

(defn get-initial-state [db]
  {:state/data {:rss (rss/get-news db)}})

(defn handle-connection [{:keys [ws-conn db-conn]
                          {:keys [bus]} :broadcaster
                          :as config}]
  (send-to-ws ws-conn (get-initial-state (d/db db-conn)))
  (ms/consume (fn [payload]
                (when-let [message (parse-client-message ws-conn payload)]
                  (handle-client-message config message)))
              ws-conn)
  (ms/connect (bus/subscribe bus :events)
              ws-conn))
