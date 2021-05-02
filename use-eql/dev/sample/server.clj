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
            [clojure.java.io :as io])
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

(pco/defresolver conn->db [{::keys [conn]} _input]
  {::db @conn})

(pco/defresolver hello [env {::keys [db]}]
  {::pco/output [:sample.app/hello]}
  {:sample.app/hello (::v db)})

(pco/defmutation add [{::keys [conn]} {:sample.app/keys [v]}]
  {::pco/output [::db]}
  {::db (swap! conn update ::v + (or v 0))})

(def indexes
  (assoc (pci/register [hello
                        (assoc add
                          ::pco/op-name 'sample.app/add)
                        conn->db])
    ::conn (atom {::v 0})))

(defn eql-api
  [{:keys [body]
    :as   request}]
  (let [query (t/read (t/reader body :json))
        result (p.eql/process (merge request indexes)
                              query)]
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

