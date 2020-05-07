(ns home.http
  (:require [aleph.http :as http]
            [aleph.netty :refer [wait-for-close]]
            [compojure.core :refer [GET routes]]
            [home.html :as html]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [content-type response]]))

(def app-routes
  (routes
   (GET "/" [] (-> (response html/page)
                   (content-type "text/html")))))

(def app
  (-> app-routes
      (wrap-resource "public")))

(defn start [{:keys [port join?]}]
  (let [server (http/start-server app
                                  {:port port})]
    (when join?
      (wait-for-close server))
    server))

(defn stop [server]
  (.close server))
