(ns home.rss-test
  (:require [clojure.test :refer :all]
            [home.db.rss :as db.rss]
            [home.rss :as rss]
            [home.test :refer :all]
            [home.websocket.utils :refer [take-from-ws]]))

(use-fixtures :each with-db with-ws)

(defn ws-config []
  {:db-conn @db-conn
   :ws-conn @ws-conn})

(deftest synchronize-rss-test
  (let [[ved-id med-id] (create-rss-feeds)
        command-id (random-uuid)]
    (testing "with valid params"
      (rss/synchronize-rss (ws-config)
                           [{:rss/name "lenta.ru"
                             :rss/url (local-file-url "lenta.xml")
                             :foo/bar :baz}
                            {:rss/id ved-id
                             :rss/name "Vedomosti"
                             :rss/url (local-file-url "meduza.xml")
                             :foo/bar :baz}]
                           command-id)
      (let [updated-feeds (db.rss/list-rss (db))
            ids (->> updated-feeds (map :rss/id) set)]
        (is (= 2 (count ids)))
        (is (not (ids med-id)))
        (is (ids ved-id))
        (is (= #{"lenta.ru" "Vedomosti"}
               (->> updated-feeds
                    (map :rss/name)
                    set)))
        (is (= #{(local-file-url "lenta.xml")
                 (local-file-url "meduza.xml")}
               (->> updated-feeds
                    (map :rss/url)
                    set)))
        (is (nil? (take-from-ws (server->client))))))
    (testing "with duplicate :rss/url"
      (rss/synchronize-rss (ws-config)
                           [{:rss/name "First RSS"
                             :rss/url "https://example.com"}
                            {:rss/name "Second RSS"
                             :rss/url "https://example.com"}]
                           command-id)
      (is (= {:command/id command-id
              :command/error "Some urls are not unique."}
             (take-from-ws (server->client)))))
    (testing "with invalid params"
      (rss/synchronize-rss (ws-config)
                           [{:rss/name "foobar"}
                            {:rss/id 1
                             :rss/url "foo://bar.baz"}]
                           command-id)
      (let [{:command/keys [id error]} (take-from-ws (server->client))]
        (is (= command-id id))
        (is (some? (re-find #":rss/id" error)))))
    (testing "with non-rss url"
      (rss/synchronize-rss (ws-config)
                           [{:rss/name "Invalid"
                             :rss/url "https://httpbin.org/ip"}]
                           command-id)
      (is (= {:command/id command-id
              :command/error "This is not an RSS url."}
             (take-from-ws (server->client)))))
    (testing "with 404 url"
      (rss/synchronize-rss (ws-config)
                           [{:rss/name "Not Found"
                             :rss/url "https://httpbin.org/i-dont-exist"}]
                           command-id)
      (is (= {:command/id command-id
              :command/error "This is not an RSS url."}
             (take-from-ws (server->client)))))
    (testing "with unknown host"
      (rss/synchronize-rss (ws-config)
                           [{:rss/name "Unknown host"
                             :rss/url "https://foo.bar"}]
                           command-id)
      (is (= {:command/id command-id
              :command/error "This is not an RSS url."}
             (take-from-ws (server->client)))))))

(deftest get-news-test
  (let [[ved-id med-id] (create-rss-feeds)
        news (rss/get-news (db))]
    (is (= #{{:rss/id ved-id
              :rss/name "Vedomosti"
              :rss/url (local-file-url "vedomosti.xml")
              :rss/news (-> "vedomosti.xml" local-file-url rss/load-news)}
             {:rss/id med-id
              :rss/name "Meduza"
              :rss/url (local-file-url "meduza.xml")
              :rss/news (-> "meduza.xml" local-file-url rss/load-news)}}
           (set news)))))

(deftest serialize-test
  (let [[ved-id] (create-rss-feeds)
        feed (rss/serialize (db) ved-id)]
    (is (= {:rss/id ved-id
            :rss/name "Vedomosti"
            :rss/url (local-file-url "vedomosti.xml")
            :rss/news (-> "vedomosti.xml" local-file-url rss/load-news)}
           feed))))
