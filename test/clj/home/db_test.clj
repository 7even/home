(ns home.db-test
  (:require [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [datomic.api :as d]
            [home.db :as db]
            [home.rss :as rss]
            [home.test :refer :all])
  (:import java.util.concurrent.TimeUnit))

(use-fixtures :each with-db)

(defn- vec->datom [[e a v tx added?]]
  {:e e
   :a a
   :v v
   :tx tx
   :added added?})

(defn- tx-report [f]
  (try
    (let [queue (db/get-queue @db-conn)]
      (f)
      (.poll queue 50 TimeUnit/MILLISECONDS))
    (finally (db/remove-queue @db-conn))))

(s/def :change/name
  #{:rss/created :rss/updated :rss/deleted})

(s/def :change/data
  map?)

(s/def ::change
  (s/keys :req [:change/name :change/data]))

(s/def :command/id
  uuid?)

(s/def :event/happened-at
  (and inst?
       #(t/within? (t/minus (t/now) (t/seconds 5))
                   (t/now)
                   (c/from-date %))))

(s/def :event/changes
  (s/coll-of ::change
             :kind vector?))

(s/def ::event
  (s/keys :req [:command/id :event/happened-at :event/changes]))

(deftest format-event-test
  (let [ved-id (d/squuid)
        med-id (d/squuid)
        len-id (d/squuid)
        command-id (random-uuid)
        tx-attrs {:db/id "datomic.tx"
                  :command/id command-id}]
    (testing "with changes from a created entity"
      (let [report (tx-report #(d/transact @db-conn
                                           [{:rss/id ved-id
                                             :rss/name "Vedomosti"
                                             :rss/url (local-file-url "vedomosti.xml")}
                                            tx-attrs]))
            event (db/format-event report)]
        (is (s/valid? ::event event))
        (is (= command-id (:command/id event)))
        (is (= [{:change/name :rss/created
                 :change/data (rss/serialize (db) ved-id)}]
               (:event/changes event)))))
    (testing "with changes from an updated entity"
      (let [report (tx-report #(d/transact @db-conn
                                           [{:rss/id ved-id
                                             :rss/url (local-file-url "meduza.xml")}
                                            tx-attrs]))
            event (db/format-event report)]
        (is (s/valid? ::event event))
        (is (= command-id (:command/id event)))
        (is (= [{:change/name :rss/updated
                 :change/data (-> (rss/serialize (db) ved-id)
                                  (select-keys [:rss/id :rss/url :rss/news]))}]
               (:event/changes event)))))
    (testing "with changes from a deleted entity"
      (let [report (tx-report #(d/transact @db-conn
                                           [[:db/retractEntity [:rss/id ved-id]]
                                            tx-attrs]))
            event (db/format-event report)]
        (is (s/valid? ::event event))
        (is (= command-id (:command/id event)))
        (is (= [{:change/name :rss/deleted
                 :change/data {:rss/id ved-id}}]
               (:event/changes event)))))
    (testing "with changes covering several entities"
      (create-rss {:rss/id med-id
                   :rss/name "Meduza"})
      (create-rss {:rss/id len-id
                   :rss/name "Lenta"
                   :rss/url (local-file-url "lenta.xml")})
      (let [report (tx-report (fn []
                                (d/transact @db-conn
                                            [{:rss/id ved-id
                                              :rss/name "Vedomosti"
                                              :rss/url (local-file-url "vedomosti.xml")}
                                             {:rss/id med-id
                                              :rss/url (local-file-url "meduza.xml")}
                                             [:db/retractEntity [:rss/id len-id]]
                                             tx-attrs])))
            event (db/format-event report)]
        (is (s/valid? ::event event))
        (is (= command-id (:command/id event)))
        (is (= [{:change/name :rss/created
                 :change/data (rss/serialize (db) ved-id)}
                {:change/name :rss/updated
                 :change/data (-> (rss/serialize (db) med-id)
                                  (select-keys [:rss/id :rss/url :rss/news]))}
                {:change/name :rss/deleted
                 :change/data {:rss/id len-id}}]
               (:event/changes event)))))))
