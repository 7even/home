(ns home.core
  (:require [aero.core :as aero]
            [clojure.java.io :refer [resource]]))

(def app-env
  (or (keyword (System/getenv "APP_ENV"))
      :development))

(defn config
  ([] (config app-env))
  ([profile]
   (-> "config.edn"
       resource
       (aero/read-config {:profile profile}))))
