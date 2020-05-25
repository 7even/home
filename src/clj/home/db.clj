(ns home.db
  (:require [clojure.data :refer [diff]]
            [datomic.api :as d]
            [home.rss :as rss]
            [io.rkn.conformity :as conf]))

(defn setup-db [uri]
  (d/create-database uri)
  (let [conn (d/connect uri)
        norms (conf/read-resource "schema.edn")]
    (conf/ensure-conforms conn norms [:home/schema])
    conn))

(def get-queue d/tx-report-queue)

(def remove-queue d/remove-tx-report-queue)

(defn- get-ident [db id]
  (:db/ident (d/pull db [:db/ident] id)))

(def serializers
  {"rss" rss/serialize})

(defn- serialize-event [attrs e db-before db-after]
  (let [id-attr (->> attrs
                     keys
                     (filter #(= (name %) "id"))
                     first)
        [id entity-added?] (first (get attrs id-attr))
        entity-type (-> attrs keys first namespace)
        serializer (serializers entity-type)]
    (if (some? id-attr)
      (if entity-added?
        {:event/name (keyword entity-type "created")
         :event/data (serializer db-after id)}
        {:event/name (keyword entity-type "deleted")
         :event/data {id-attr id}})
      (let [entity-id-attr (keyword entity-type "id")
            entity-id (entity-id-attr (d/pull db-after [entity-id-attr] e))]
        {:event/name (keyword entity-type "updated")
         :event/data (merge {entity-id-attr entity-id}
                            (second (diff (serializer db-before entity-id)
                                          (serializer db-after entity-id))))}))))

(defn format-changes [{:keys [db-before db-after tx-data]}]
  (let [normalized (->> tx-data
                        (group-by :e)
                        (map (fn [[e datoms]]
                               [(reduce (fn [attrs {:keys [a v added]}]
                                          (update attrs
                                                  (get-ident db-after a)
                                                  #(conj % [v added])))
                                        {}
                                        datoms)
                                e])))
        grouped (group-by (fn [[attrs _]]
                            (contains? attrs :db/txInstant))
                          normalized)
        tx (ffirst (get grouped true))
        entities (get grouped false)]
    (mapv (fn [[attrs e]]
            (merge (serialize-event attrs e db-before db-after)
                   {:event/happened-at (-> tx :db/txInstant ffirst)}
                   (when (contains? tx :command/id)
                     {:command/id (-> tx :command/id ffirst)})))
          entities)))

(comment
  (d/delete-database (get-in (home.core/config)
                             [:datomic/client :uri])))
