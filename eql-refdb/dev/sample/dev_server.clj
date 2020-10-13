(ns sample.dev-server
  (:require [io.pedestal.http :as http]
            [shadow.cljs.devtools.api :as shadow.api]
            [shadow.cljs.devtools.server :as shadow.server]
            [hiccup2.core :as h]
            [ring.util.mime-type :as mime])
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
                   (h/html
                     {:mode :html}
                     (h/raw "<!DOCTYPE html>"))
                   str)
     :headers {"Content-Type"            (mime/default-mime-types "html")
               "Content-Security-Policy" ""}
     :status  200}))
(def routes
  `#{["/" :get index]})

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
