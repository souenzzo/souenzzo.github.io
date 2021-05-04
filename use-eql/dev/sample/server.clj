(ns sample.server
  (:require [io.pedestal.http :as http]
            [shadow.cljs.devtools.api :as shadow.api]
            [shadow.cljs.devtools.server :as shadow.server]
            [hiccup2.core :as h]
            [ring.util.mime-type :as mime]
            [cheshire.core :as json]
            [cognitect.transit :as t]
            [com.wsscode.pathom3.connect.operation :as pco]
            [com.wsscode.pathom3.connect.indexes :as pci]
            [com.wsscode.pathom3.connect.built-in.resolvers :as pbir]
            [com.wsscode.pathom3.interface.eql :as p.eql]
            [clojure.java.io :as io]
            [com.wsscode.pathom.connect :as pc])
  (:import (java.nio.charset StandardCharsets)))

(defn index
  [req]
  (let [html [:html
              [:head
               [:meta {:charset (str StandardCharsets/UTF_8)}]
               [:link {:href "data:image/svg+xml" :rel "icon"}]
               [:title "sample"]]
              [:body
               {:onload "sample.app.start('app')"}
               [:section
                {:id "app"}]
               [:script
                {:src "/sample/client.js"}]]]]
    {:body    (->> html
                (h/html {:mode :html})
                (str "<!DOCTYPE html>\n"))
     :headers {"Content-Type"            (mime/default-mime-types "html")
               "Content-Security-Policy" ""}
     :status  200}))

(pco/defresolver conn->db [{::keys [*counter]} _input]
  {:sample.app/current-count @*counter})

(pco/defmutation increment [{::keys [*counter]} _params]
  {:sample.app/current-count (swap! *counter inc)})


(pco/defresolver todo-db [{::keys [*todo-db]} _]
  {::todo-db @*todo-db})

(pco/defresolver todo-by-id [{:app.todo/keys [id]
                              ::keys         [todo-db]}]
  {::pc/output [:app.todo/text]}
  (get todo-db id))


(pco/defresolver todos [{::keys [todo-db]}]
  {:app.todo/todos (vals todo-db)})

(pco/defmutation new-todo [{::keys [*todo-db]} {:app.todo/keys [text]}]
  {::pc/params [:app.todo/text]
   ::pc/output [:app.todo/id
                ::todo-db]}
  (let [id (str (gensym "todo"))
        todo-db (swap! *todo-db assoc id {:app.todo/text text
                                          :app.todo/id   id})]
    (assoc (get todo-db id)
      ::todo-db todo-db)))


(pco/defmutation remove-todo [{::keys [*todo-db]} {:app.todo/keys [id]}]
  {::pc/params [:app.todo/text]
   ::pc/output [:app.todo/id
                ::todo-db]}
  (let [[todo-db-before todo-db] (swap-vals! *todo-db dissoc id)]
    (assoc (get todo-db-before id)
      ::todo-db todo-db)))



(def indexes
  (assoc (pci/register [(update increment :config
                          assoc
                          ::pco/op-name 'sample.app/increment)
                        todos todo-by-id todo-db
                        (update new-todo :config
                          assoc ::pco/op-name 'app.todo/new-todo)
                        (update remove-todo :config
                          assoc ::pco/op-name 'app.todo/remove)
                        conn->db])
    ::*todo-db (atom {})
    ::*counter (atom 0)))

(defn eql-api
  [{:keys [body]
    :as   request}]
  (let [query (t/read (t/reader body :json))
        result (p.eql/process (merge request indexes)
                 query)]
    (prn {:query  query
          :result result})
    {:body   (fn [out]
               (let [out (t/writer out :json)]
                 (t/write out result)))
     :status 200}))

(def routes
  `#{["/" :get index]
     ["/api" :post eql-api]})

(defonce http-state (atom nil))

(defn -main
  []
  (shadow.server/start!)
  (shadow.api/watch :sample)
  (swap! http-state
    (fn [st]
      (some-> st http/stop)
      (-> {::http/routes     routes
           ::http/join?      false
           ::http/file-path  "target"
           ::http/mime-types mime/default-mime-types
           ::http/type       :jetty
           ::http/port       8080}
        http/default-interceptors
        http/dev-interceptors
        http/create-server
        http/start))))

