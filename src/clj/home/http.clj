(ns home.http
  (:require [aleph.http :as http]
            [aleph.netty :refer [wait-for-close]]
            [compojure.core :refer [GET routes]]
            [home.html :as html]
            [home.websocket :as ws]
            [manifold.deferred :as d]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [content-type response]]))

(defn ws-handler [config]
  (fn [req]
    (d/let-flow [ws-conn (http/websocket-connection req)
                 ws-config (assoc config :ws-conn ws-conn)]
      (ws/handle-connection ws-config)
      nil)))

(defn app-routes [config]
  (routes
   (GET "/ws" [] (ws-handler config))
   (GET "/*" [] (-> (response html/page)
                    (content-type "text/html")))))

(defn app [config]
  (-> (app-routes config)
      (wrap-resource "public")))

(defn start [{:keys [port join?] :as config}]
  (let [server (http/start-server (app config) {:port port})]
    (when join?
      (wait-for-close server))
    server))

(defn stop [server]
  (.close server))
