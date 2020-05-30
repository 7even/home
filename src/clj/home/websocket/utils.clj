(ns home.websocket.utils
  (:require [manifold.stream :as s]))

(defn send-to-ws [s payload]
  (s/put! s (pr-str payload)))

(defn take-from-ws [s]
  (when-let [message-str @(s/try-take! s 20)]
    (read-string message-str)))
