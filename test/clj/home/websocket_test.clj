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
        event (take-from-ws (server->client))]
    (is (= 1 (count feeds)))
    (is (= "Vedomosti"
           (:rss/name feed)))
    (is (= (local-file-url "vedomosti.xml")
           (:rss/url feed)))
    (is (= #{:command/id :event/happened-at :event/changes}
           (-> event keys set)))
    (is (= command-id (:command/id event)))
    (is (= 1 (-> event :event/changes count)))
    (let [change (-> event :event/changes first)]
      (is (= :rss/created (:change/name change)))
      (is (= "Vedomosti" (get-in change [:change/data :rss/name])))
      (is (= (local-file-url "vedomosti.xml")
             (get-in change [:change/data :rss/url])))
      (is (= (rss/load-news (local-file-url "vedomosti.xml"))
             (get-in change [:change/data :rss/news]))))))
