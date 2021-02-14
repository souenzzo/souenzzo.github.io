(ns br.com.souenzzo.mpr.main
  (:require [clojure.string :as string]
            [datomic.api :as d]
            [br.com.souenzzo.dvm :as dvm]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.csrf :as csrf]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [io.pedestal.http.route :as route]
            [io.pedestal.log :as log]
            [ring.middleware.session.store :as session.store]
            [ring.util.mime-type :as mime])
  (:import (java.nio.charset StandardCharsets)
           (java.util UUID)))

(defn href [route-name & opts]
  (let [{:as this} opts]
    (with-meta (assoc this :route-name route-name)
               `{dvm/-render ~(fn [_ _]
                                (apply route/url-for route-name opts))})))

(defn ui-std-head
  [req opts]
  [:head
   [:meta {:charset (str StandardCharsets/UTF_8)}]
   [:link {:rel "icon" :href "data:"}]
   [:title "mpr"]])

(defn index
  [req]
  (let []
    {:body    (->> [:html
                    [ui-std-head {}]
                    [:body
                     "hello"]]
                   (dvm/render-to-string req)
                   (conj ["<!DOCTYPE html>"])
                   (string/join "\n"))
     :headers {"Content-Type" (mime/default-mime-types "html")}
     :status  200}))

(defn login!
  [{::keys      [conn]
    ::csrf/keys [anti-forgery-token]
    :keys       [form-params]}]
  (let [{:keys [email next]} form-params
        operator (d/tempid :db.part/user)]
    @(d/transact conn [{:mpr.session/anti-forgery-token anti-forgery-token
                        :mpr.session/operador           operator
                        :mpr.session/authed?            true}
                       {:db/id              operator
                        :mpr.operator/email email}])
    {:headers (merge {}
                     (when next
                       {"Location" next}))
     :status  303}))

(defn ui-login-form
  [{::csrf/keys [anti-forgery-token]
    :keys       [query-params]} opts]
  (let [{:keys [next]} query-params]
    [:form
     {:action (href :login!)
      :method "POST"}
     "login"
     [:label
      "email"
      [:input {:name "email"}]]
     [:input {:type "submit"}]
     [:input {:hidden true
              :name   csrf/anti-forgery-token-str
              :value  anti-forgery-token}]
     (when next
       [:input {:hidden true
                :name   "next"
                :value  next}])]))

(defn login
  [req]
  {:body    (->> [:html
                  [ui-std-head {}]
                  [:body
                   [ui-login-form {}]]]
                 (dvm/render-to-string req)
                 (conj ["<!DOCTYPE html>"])
                 (string/join "\n"))
   :headers {"Content-Type" (mime/default-mime-types "html")}
   :status  200})

(def auth
  {:name  ::auth
   :enter (fn [{{::csrf/keys [anti-forgery-token]
                 ::keys      [db]} :request
                :as                ctx}]
            (when-not (:mpr.session/authed? (d/entity db [:mpr.session/anti-forgery-token anti-forgery-token]))
              (->> {:cognitect.anomalies/category :cognitect.anomalies/forbidden}
                   (ex-info "Not authed")
                   throw))
            ctx)})

(def ex-handler
  {:name  ::ex-handler
   :error (fn [{:keys [request]
                :as   ctx} ex]
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
                                   :status 500}))))})

(defn conn-session-store
  [conn]
  (reify session.store/SessionStore
    (read-session [this id]
      (d/pull (d/db conn)
              `[(:mpr.session/anti-forgery-token :as ~csrf/anti-forgery-token-str)]
              [:mpr.session/id id]))
    (write-session [this id* data]
      (let [anti-forgery-token (get data csrf/anti-forgery-token-str)
            ;; this key need to by crypto-safe
            id (or id* (str (UUID/randomUUID)))
            session (d/tempid :db.part/user)]
        @(d/transact conn (concat [[:db/add session :mpr.session/id id]]
                                  (when anti-forgery-token
                                    [[:db/add session :mpr.session/anti-forgery-token anti-forgery-token]])))
        id))))

(defn routes
  [{::keys [conn]
    :as    env}]
  (let [store (conn-session-store conn)
        session (middlewares/session {:store       store
                                      :cookie-name "mpr"})
        csrf (csrf/anti-forgery {})
        entity-conn {:name  ::entity-conn
                     :enter (fn [ctx]
                              (if (-> ctx :request :request-method #{:post})
                                (update ctx :request assoc
                                        ::db (d/db conn)
                                        ::conn conn)
                                (update ctx :request assoc
                                        ::db (d/db conn))))}
        body-params (body-params/body-params)]
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
            :route-name :login]
           ["/login" :post ~[ex-handler
                             session
                             body-params
                             csrf
                             entity-conn
                             login!]
            :route-name :login!]}
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
    :db/cardinality :db.cardinality/one}
   {:db/ident       :mpr.session/authed?
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}
   {:db/ident       :mpr.session/operador
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident       :mpr.operator/email
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
