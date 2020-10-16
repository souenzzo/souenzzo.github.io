(ns br.com.souenzzo.inga.dev-server
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [ring.util.mime-type :as mime]
            [br.com.souenzzo.inga.ring :as ir]
            [com.wsscode.pathom.connect :as pc])
  (:import (java.nio.charset StandardCharsets)))

(defn index
  [req]
  {::ir/body [:html
              [:head
               [:meta {:charset (str StandardCharsets/UTF_8)}]
               [:link {:rel "icon" :href "data:"}]
               [:title "ingá!"]]
              [:body
               [::hello {}]]]
   :headers  {"Content-Security-Policy" ""
              "Content-Type"            (mime/default-mime-types "html")}
   :status   200})

(pc/defresolver hello [_ _]
  {::hello [:p "hello!!!"]})

(def indexes
  (pc/register {} [hello]))

(def connect (ir/interceptor {::pc/indexes indexes}))

(def routes
  `#{["/" :get [connect
                index]]})


(defonce state (atom nil))

(defn -main
  [& _]
  (swap! state
         (fn [st]
           (some-> st http/stop)
           (-> {::http/routes (fn []
                                (route/expand-routes @(requiring-resolve `routes)))
                ::http/port   8080
                ::http/join?  false
                ::http/type   :jetty}
               http/default-interceptors
               http/dev-interceptors
               http/create-server
               http/start))))
