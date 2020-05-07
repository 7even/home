(ns home.http
  (:require [aleph.http :as http]
            [aleph.netty :refer [wait-for-close]]
            [compojure.core :refer [GET routes]]
            [home.html :as html]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [content-type response]]))

(defn- send-to-ws [s payload]
  (s/put! s (pr-str payload)))

(defn ws-handler [req]
  (d/let-flow [conn (http/websocket-connection req)]
    (send-to-ws conn {:message "Hello!"})
    (s/consume (fn [payload]
                 (let [client-message (read-string payload)]
                   (send-to-ws conn {:you/sent client-message})))
               conn)
    nil))

(def app-routes
  (routes
   (GET "/ws" [] ws-handler)
   (GET "/*" [] (-> (response html/page)
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
