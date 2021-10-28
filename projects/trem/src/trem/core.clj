(ns trem.core
  (:require [io.pedestal.http :as http]
            [hiccup2.core :as h]
            [ring.util.mime-type :as mime]
            [io.pedestal.http.route :as route]
            [clojure.spec.alpha :as s]
            [com.wsscode.pathom3.connect.indexes :as pci])
  (:import (java.nio.charset StandardCharsets)
           (org.eclipse.jetty.servlet ServletContextHandler)
           (org.eclipse.jetty.server.handler.gzip GzipHandler)))

;; Kind of "Resource" + route "Prefix"
(s/def ::route-name keyword?)

;; Rails.application.routes.draw
(s/def ::routes-draw (s/coll-of (s/keys :req [::view
                                              ::controller
                                              ::route-name])))

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

(s/fdef welcome
  :args (s/cat :request map?))

(defn path
  [route-name params]
  (route/url-for route-name :params params))

(s/fdef path
  :args (s/cat :route-name ::route-name
          :params map?))

(defn redirect
  [route-name params]
  (let [target (route/url-for route-name :params params)]
    (throw (ex-info "Redirect" {::target target}))))

(defn form-label
  [& _]
  [:label {:for ""}])

(defn form-text-field
  [& _]
  [:input {:type "text"
           :name ""
           :id   ""}])

(defn form-with
  [& body]
  (into [:form {:method         "POST"
                :action         "/"
                :accept-charset (str StandardCharsets/UTF_8)}
         [:input {:type  "hidden"
                  :name  "authenticity_token"
                  :value ""}]
         body]))

(defn form-text-area
  [& _]
  [:textarea {:name ""
              :id   ""}])
(defn form-submit
  [& _]
  [:input {:type              "submit"
           :name              "commit"
           :value             "Create Article"
           :data-disable-with "Create Article"}])

(defn link-to
  [& _]
  [:a {:href "/"}
   "TODO"])

(defn stop
  [server]
  (http/stop server))

(s/fdef stop
  :args (s/cat :server map?))

(defn expand-draw-routes
  [{::keys [routes-draw operations]
    :as    service-map}]
  (let [indexes (pci/register operations)
        routes (for [{::keys [view controller route-name]
                      :keys  [uri request-method]
                      :or    {controller identity}}
                     routes-draw]
                 [uri request-method [{:error (fn [ctx ex]
                                                (let [{::keys [target]} (ex-data (ex-cause ex))]
                                                  (if target
                                                    (assoc ctx :response {:headers {"Location" target}
                                                                          :status  303})
                                                    (assoc ctx
                                                      :io.pedestal.interceptor.chain/error ex))))}
                                      (fn [req]
                                        (if view
                                          (let [body (-> req
                                                       (merge indexes)
                                                       (controller)
                                                       (view))]
                                            {:body    (->> [:html
                                                            [:meta {:charset (str StandardCharsets/UTF_8)}]
                                                            [:title "Trem 🚂"]
                                                            [:body body]]
                                                        (h/html {:mode :html})
                                                        (str "<!DOCTYPE html>\n"))
                                             :headers {"Content-Type" (mime/default-mime-types "html")}
                                             :status  200})
                                          (let [body (-> req
                                                       (merge indexes)
                                                       (controller))]
                                            {:status 202})))]
                  :route-name route-name])
        expanded-routes (route/expand-routes (set routes))
        index? (route/try-routing-for expanded-routes :map-tree "/" :get)]
    (if index?
      expanded-routes
      (concat expanded-routes
        (expand-draw-routes (assoc service-map
                              ::routes-draw [{:request-method :get
                                              :uri            "/"
                                              ::route-name    ::welcome
                                              ::view          (fn [_]
                                                                [:h1 "Welcome to 🚂"])}]))))))

(s/fdef expand-draw-routes
  :args (s/cat :service-map (s/keys :req [::routes-draw])))

(defn start
  [service-map]
  (let [routes (expand-draw-routes service-map)]
    (-> {::http/type              :jetty
         ::http/join?             false
         ::http/routes            routes
         ::http/container-options {:context-configurator (fn [^ServletContextHandler context]
                                                           (let [gzip-handler (GzipHandler.)]
                                                             (.addIncludedMethods gzip-handler (make-array String 0))
                                                             (.setExcludedAgentPatterns gzip-handler (make-array String 0))
                                                             (.setGzipHandler context gzip-handler))
                                                           context)}
         ::http/port              8080}
      http/default-interceptors
      http/dev-interceptors
      http/create-server
      http/start)))

(s/fdef start
  :args (s/cat :service-map (s/keys)))
