(ns br.com.souenzzo.mpr.main
  (:require [clojure.string :as string]
            [datomic.api :as d]
            [hiccup2.core :as h]
            [io.pedestal.http :as http]
            [io.pedestal.http.csrf :as csrf]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [io.pedestal.http.route :as route]
            [io.pedestal.log :as log]
            [ring.middleware.session.store :as session.store]
            [ring.util.mime-type :as mime])
  (:import (java.nio.charset StandardCharsets)
           (java.util UUID)))

(defn index
  [_]
  (let []
    {:body    (->> [:html
                    [:head
                     [:meta {:charset (str StandardCharsets/UTF_8)}]
                     [:link {:rel "icon" :href "data:"}]
                     [:title "mpr"]]
                    [:body
                     "hello"]]
                   (h/html {:mode :html})
                   (conj ["<!DOCTYPE html>"])
                   (string/join "\n"))
     :headers {"Content-Type" (mime/default-mime-types "html")}
     :status  200}))

(defn login
  [{::csrf/keys [anti-forgery-token]}]
  (let []
    {:body    (->> [:html
                    [:head
                     [:meta {:charset (str StandardCharsets/UTF_8)}]
                     [:link {:rel "icon" :href "data:"}]
                     [:title "mpr"]]
                    [:body
                     [:form
                      {:method "POST"}
                      "login"
                      [:label
                       "email"
                       [:input {:name "email"}]]
                      [:input {:type "submit"}]
                      [:input {:hidden true
                               :name   csrf/anti-forgery-token
                               :value  anti-forgery-token}]]]]
                   (h/html {:mode :html})
                   (conj ["<!DOCTYPE html>"])
                   (string/join "\n"))
     :headers {"Content-Type" (mime/default-mime-types "html")}
     :status  200}))

(defn routes
  [{::keys [conn]
    :as    env}]
  (let [ex-handler {:name  ::ex-handler
                    :error (fn [{:keys [request route]
                                 :as   ctx} ex]
                             (def __route route)
                             (cond
                               (-> ex ex-data :cognitect.anomalies/category
                                   #{:cognitect.anomalies/forbidden})
                               (assoc ctx :response
                                          {:headers {"Location" (route/url-for :login
                                                                               :params {:next (:path-info request)})}
                                           :status  303})
                               :else (let [id (d/squuid)]
                                       (log/error
                                         :unknow-exception id
                                         :exception ex)
                                       (assoc ctx
                                         :response {:id     id
                                                    :msg    (ex-message ex)
                                                    :status 500}))))}
        store (reify session.store/SessionStore
                (read-session [this id]
                  (d/pull (d/db conn)
                          `[(:mpr.session/anti-forgery-token :as ~csrf/anti-forgery-token-str)]
                          [:mpr.session/id id]))
                (write-session [this id data]
                  (let [anti-forgery-token (get data csrf/anti-forgery-token-str)
                        ;; this key need to by crypto-safe
                        id (or id (str (UUID/randomUUID)))]
                    @(d/transact conn [{:mpr.session/anti-forgery-token anti-forgery-token
                                        :mpr.session/id                 id}])
                    id)))
        session (-> {:store       store
                     :cookie-name "mpr"}
                    middlewares/session)
        csrf (-> {:read-token (fn [{:keys [path-params]}]
                                (log/info :path-params path-params)
                                nil)}
                 csrf/anti-forgery)
        entity-conn {:name  ::entity-conn
                     :enter (fn [ctx]
                              (assoc-in ctx [:request ::db] (d/db conn)))}
        auth {:name  ::auth
              :enter (fn [{{::csrf/keys [anti-forgery-token]
                            ::keys      [db]} :request
                           :as                ctx}]
                       (when-not (:mpr.session/authed? (d/entity db [:mpr.session/anti-forgery-token anti-forgery-token]))
                         (->> {:cognitect.anomalies/category :cognitect.anomalies/forbidden}
                              (ex-info "Not authed")
                              throw))
                       ctx)}]
    (-> `#{["/" :get ~[ex-handler
                       session
                       csrf
                       entity-conn
                       auth
                       index]
            :route-name :index]
           ["/login" :get ~[ex-handler
                            session
                            csrf
                            entity-conn
                            login]
            :route-name :login]}
        route/expand-routes)))

(defonce state (atom nil))

(def tx-schema
  [{:db/ident       :mpr.session/id
    :db/unique      :db.unique/identity
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :mpr.session/anti-forgery-token
    :db/unique      :db.unique/identity
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])

(defn -main
  [& _]
  (let [conn (d/connect (doto "datomic:mem://mpr"
                          #_d/delete-database
                          d/create-database))
        schema-db (-> conn
                      (d/transact tx-schema)
                      :db-after)]
    (swap! state (fn [st]
                   (some-> st http/stop)
                   (-> {::http/port   8080
                        ::http/routes (fn []
                                        (routes {::conn      conn
                                                 ::schema-db schema-db}))
                        ::http/type   :jetty
                        ::http/join?  false}
                       http/default-interceptors
                       http/dev-interceptors
                       http/create-server
                       http/start)))))
