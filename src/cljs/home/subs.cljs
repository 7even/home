(ns home.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub ::rss
            (fn [db]
              (:rss db)))
