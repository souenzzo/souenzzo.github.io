(ns trem.core
  (:require [io.pedestal.http :as http]
            [hiccup2.core :as h]
            [ring.util.mime-type :as mime]
            [io.pedestal.http.route :as route]
            [clojure.spec.alpha :as s])
  (:import (java.nio.charset StandardCharsets)))

;; Kind of "Resource" + route "Prefix"
(s/def ::route-name keyword?)

;; Rails.application.routes.draw
(s/def ::routes-draw coll?)

;; app/views/articles/(fn [_]).html.erb
(s/def ::view fn?)
;; app/controllers/:resource_controller.rb(fn [_])
(s/def ::controller fn?)

(defn welcome
  [_]
  {:body    (->> [:html
                  [:meta {:charset (str StandardCharsets/UTF_8)}]
                  [:title "Trem 🚂"]
                  [:body
                   [:h1 "Welcome to 🚂"]]]
              (h/html {:mode :html})
              (str "<!DOCTYPE html>\n"))
   :headers {"Content-Type" (mime/default-mime-types "html")}
   :status  200})

(defn path
  [route-name params]
  (route/url-for route-name :params params))

(defn stop
  [server]
  (http/stop server))

(defn expand-draw-routes
  [{::keys [routes-draw]}]
  (let [routes (for [{::keys [view controller route-name]
                      :keys  [uri request-method]} routes-draw]
                 [uri request-method (fn [req]
                                       (let [body (-> req
                                                    (controller)
                                                    (view))]
                                         {:body    (->> [:html
                                                         [:meta {:charset (str StandardCharsets/UTF_8)}]
                                                         [:title "Trem 🚂"]
                                                         [:body body]]
                                                     (h/html {:mode :html})
                                                     (str "<!DOCTYPE html>\n"))
                                          :headers {"Content-Type" (mime/default-mime-types "html")}
                                          :status  200}))
                  :route-name route-name])]
    (route/expand-routes (set routes))))

(defn start
  [{::keys [routes-draw]
    :as    service-map}]
  (-> {::http/type   :jetty
       ::http/join?  false
       ::http/routes (if routes-draw
                       (expand-draw-routes service-map)
                       `#{["/" :get welcome]})
       ::http/port   8080}
    http/default-interceptors
    http/create-server
    http/start))
