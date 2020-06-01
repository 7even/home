(ns home.websocket-test
  (:require [clojure.test :refer :all]
            [home.db.rss :as db.rss]
            [home.rss :as rss]
            [home.test :refer :all]
            [home.websocket :as ws]
            [home.websocket.utils :refer [send-to-ws take-from-ws]]
            [manifold.stream :as s]))

(defn- ws-config []
  {:ws-conn @ws-conn
   :db-conn @db-conn
   :broadcaster {:bus @bus}})

(use-fixtures :each with-db with-broadcaster with-ws)

(deftest handle-connection-test
  (create-rss-feeds)
  (ws/handle-connection (ws-config))
  (is (= {:state/data {:rss (rss/get-news (db))}}
         (take-from-ws (server->client)))))

(deftest parse-client-message-test
  (testing "with invalid edn"
    (ws/handle-connection (ws-config))
    (take-from-ws (server->client))
    (s/put! (client->server) "\"foo")
    (is (= "Failed to parse the message"
           (:message/error (take-from-ws (server->client))))))
  (testing "with invalid message"
    (ws/handle-connection (ws-config))
    (take-from-ws (server->client))
    (send-to-ws (client->server) {:foo :bar})
    (is (some? (re-find #":command/id"
                        (:message/error (take-from-ws (server->client))))))))

(deftest handle-client-message-test
  (ws/handle-connection (ws-config))
  (take-from-ws (server->client))
  (let [command-id (random-uuid)
        _ (send-to-ws (client->server)
                      {:command/id command-id
                       :command/name :rss/synchronize
                       :command/data [{:rss/name "Vedomosti"
                                       :rss/url (local-file-url "vedomosti.xml")}]})
        feeds (db.rss/list-rss (db))
        feed (first feeds)
        events-msg (take-from-ws (server->client))
        event (-> events-msg :events/data first)]
    (is (= 1 (count feeds)))
    (is (= "Vedomosti"
           (:rss/name feed)))
    (is (= (local-file-url "vedomosti.xml")
           (:rss/url feed)))
    (is (= 1 (-> events-msg :events/data count)))
    (is (= #{:event/name :event/data :event/happened-at :command/id}
           (-> event keys set)))
    (is (= :rss/created (:event/name event)))
    (is (= "Vedomosti"
           (get-in event [:event/data :rss/name])))
    (is (= (local-file-url "vedomosti.xml")
           (get-in event [:event/data :rss/url])))
    (is (= (rss/load-news (local-file-url "vedomosti.xml"))
           (get-in event [:event/data :rss/news])))))
