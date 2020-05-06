(ns home.core
  (:require [aero.core :as aero]
            [clojure.java.io :refer [resource]]
            [home.http :as http]
            [integrant.core :as ig]))

(def app-env
  (or (keyword (System/getenv "APP_ENV"))
      :development))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defn config
  ([] (config app-env))
  ([profile]
   (-> "config.edn"
       resource
       (aero/read-config {:profile profile}))))

(defmethod ig/init-key :http/handler [_ config]
  (println ";; Starting HTTP handler")
  (http/start config))

(defmethod ig/halt-key! :http/handler [_ server]
  (println ";; Stopping HTTP handler")
  (http/stop server))

(defn -main []
  (let [system-config (assoc-in (config)
                                [:http/handler :join?]
                                true)]
    (ig/init system-config)))
