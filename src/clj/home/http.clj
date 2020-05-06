(ns home.http
  (:require [aleph.http :as http]
            [aleph.netty :refer [wait-for-close]]
            [compojure.core :refer [GET routes]]
            [ring.util.response :refer [response]]))

(def app-routes
  (routes
   (GET "/" [] (response "Welcome home!"))))

(defn start [{:keys [port join?]}]
  (let [server (http/start-server app-routes
                                  {:port port})]
    (when join?
      (wait-for-close server))
    server))

(defn stop [server]
  (.close server))
