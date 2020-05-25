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

(s/def :event/name
  #{:rss/created :rss/updated :rss/deleted})

(s/def :event/data
  map?)

(s/def :event/happened-at
  (and inst?
       #(t/within? (t/minus (t/now) (t/seconds 5))
                   (t/now)
                   (c/from-date %))))

(s/def ::event
  (s/keys :req [:event/name :event/data :event/happened-at]))

(deftest format-changes-test
  (let [ved-id (d/squuid)
        med-id (d/squuid)
        len-id (d/squuid)]
    (testing "with changes from a created entity"
      (let [report (tx-report #(create-rss {:rss/id ved-id}))
            formatted-changes (db/format-changes report)]
        (is (= 1 (count formatted-changes)))
        (let [change (first formatted-changes)]
          (is (s/valid? ::event change))
          (is (= :rss/created
                 (:event/name change)))
          (is (= (rss/serialize (db) ved-id)
                 (:event/data change))))))
    (testing "with changes from an updated entity"
      (let [report (tx-report #(d/transact @db-conn
                                           [{:rss/id ved-id
                                             :rss/url (local-file-url "meduza.xml")}]))
            formatted-changes (db/format-changes report)]
        (is (= 1 (count formatted-changes)))
        (let [change (first formatted-changes)]
          (is (s/valid? ::event change))
          (is (= :rss/updated
                 (:event/name change)))
          (is (= (select-keys (rss/serialize (db) ved-id) [:rss/id :rss/url :rss/news])
                 (:event/data change))))))
    (testing "with changes from a deleted entity"
      (let [report (tx-report #(d/transact @db-conn
                                           [[:db/retractEntity [:rss/id ved-id]]]))
            formatted-changes (db/format-changes report)]
        (is (= 1 (count formatted-changes)))
        (let [change (first formatted-changes)]
          (is (s/valid? ::event change))
          (is (= :rss/deleted
                 (:event/name change)))
          (is (= {:rss/id ved-id}
                 (:event/data change))))))
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
                                             [:db/retractEntity [:rss/id len-id]]])))
            formatted-changes (db/format-changes report)]
        (is (= 3 (count formatted-changes)))
        (let [[ved med len] formatted-changes]
          (is (s/valid? ::event ved))
          (is (= :rss/created
                 (:event/name ved)))
          (is (= (rss/serialize (db) ved-id)
                 (:event/data ved)))
          (is (s/valid? ::event med))
          (is (= :rss/updated
                 (:event/name med)))
          (is (= (select-keys (rss/serialize (db) med-id)
                              [:rss/id :rss/url :rss/news])
                 (:event/data med)))
          (is (s/valid? ::event len))
          (is (= :rss/deleted
                 (:event/name len)))
          (is (= {:rss/id len-id}
                 (:event/data len))))))))
