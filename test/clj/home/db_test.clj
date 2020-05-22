(ns home.db-test
  (:require [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clojure.test :refer :all]
            [datomic.api :as d]
            [home.db :as db]
            [home.rss :as rss]
            [home.test :refer :all]))

(use-fixtures :each with-db)

(defn- vec->datom [[e a v tx added?]]
  {:e e
   :a a
   :v v
   :tx tx
   :added added?})

(deftest format-changes-test
  (let [tx-eid 1
        [ved-id med-id] (create-rss-feeds)
        ved-eid (d/entid (db) [:rss/id ved-id])
        med-eid (d/entid (db) [:rss/id med-id])
        now (c/to-date (t/now))]
    (testing "with changes from a created entity"
      (let [datoms (->> [[tx-eid  (d/entid (db) :db/txInstant) now                 tx-eid true]
                         [med-eid (d/entid (db) :rss/id)       med-id              tx-eid true]
                         [med-eid (d/entid (db) :rss/name)     "Meduza"            tx-eid true]
                         [med-eid (d/entid (db) :rss/url)      "https://meduza.io" tx-eid true]]
                        (map vec->datom))
            formatted-changes (db/format-changes {:db-after (db)
                                                  :tx-data datoms})]
        (is (= [{:event/name :rss/created
                 :event/happened-at now
                 :event/data (rss/serialize (db) med-id)}]
               formatted-changes))))
    (testing "with changes from a deleted entity"
      (let [datoms (->> [[tx-eid     (d/entid (db) :db/txInstant) now                 tx-eid true]
                         [med-eid (d/entid (db) :rss/id)          med-id              tx-eid false]
                         [med-eid (d/entid (db) :rss/name)        "Meduza"            tx-eid false]
                         [med-eid (d/entid (db) :rss/url)         "https://meduza.io" tx-eid false]]
                        (map vec->datom))
            formatted-changes (db/format-changes {:db-after (db)
                                                  :tx-data datoms})]
        (is (= [{:event/name :rss/deleted
                 :event/happened-at now
                 :event/data {:rss/id med-id}}]
               formatted-changes))))
    (testing "with changes from an updated entity"
      (let [datoms (->> [[tx-eid   (d/entid (db) :db/txInstant) now                tx-eid true]
                         [med-eid (d/entid (db) :rss/url)      "https://meduza.ru" tx-eid true]
                         [med-eid (d/entid (db) :rss/url)      "https://meduza.io" tx-eid false]]
                        (map vec->datom))
            formatted-changes (db/format-changes {:db-after (db)
                                                  :tx-data datoms})]
        (is (= [{:event/name :rss/updated
                 :event/happened-at now
                 :event/data {:rss/id med-id
                              :rss/url "https://meduza.ru"}}]
               formatted-changes))))
    (testing "with changes covering several entities"
      (let [len-eid 2
            len-id (d/squuid)
            datoms (->> [[tx-eid      (d/entid (db) :db/txInstant)         now        tx-eid true]
                         [ved-eid (d/entid (db) :rss/id)              ved-id tx-eid true]
                         [ved-eid (d/entid (db) :rss/name)           "Vedomosti" tx-eid true]
                         [ved-eid (d/entid (db) :rss/url) "https://vedomosti.ru" tx-eid true]
                         [med-eid  (d/entid (db) :rss/url)         "https://meduza.ru" tx-eid true]
                         [len-eid (d/entid (db) :rss/id)            len-id tx-eid false]
                         [len-eid (d/entid (db) :rss/name)         "Lenta" tx-eid false]
                         [len-eid (d/entid (db) :rss/url)    "https://lenta.ru" tx-eid false]]
                        (map vec->datom))
            formatted-changes (db/format-changes {:db-after (db)
                                                  :tx-data datoms})]
        (is (= [{:event/name :rss/created
                 :event/happened-at now
                 :event/data (rss/serialize (db) ved-id)}
                {:event/name :rss/updated
                 :event/happened-at now
                 :event/data {:rss/id med-id
                              :rss/url "https://meduza.ru"}}
                {:event/name :rss/deleted
                 :event/happened-at now
                 :event/data {:rss/id len-id}}]
               formatted-changes))))))
