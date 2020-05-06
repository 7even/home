(ns user
  (:require [home.core :refer [config]]
            [integrant.repl :refer [go halt]]))

(integrant.repl/set-prep! #(config :development))

(defn system []
  integrant.repl.state/system)

(def reset integrant.repl/reset)
