(ns sample.dev-app
  (:require [sample.app :as app]
            [reagent.dom :as rd]
            [goog.dom :as gdom]
            [re-frame.core :as rf]))


(defn after-load
  []
  (rf/clear-subscription-cache!)
  (rd/render [app/ui] (gdom/getElement "app")))
