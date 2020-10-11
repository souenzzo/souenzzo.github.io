(ns br.com.souenzzo.dvm.dev
  (:require [io.pedestal.http :as http]
            [br.com.souenzzo.dvm :as dvm]
            [io.pedestal.http.route :as route]
            [ring.util.mime-type :as mime]))

(def ui-hello
  (dvm/el "html" {}
          (dvm/el "head" {}
                  (dvm/el "title" {}
                          "hello!"))
          (dvm/el "body" {}
                  (dvm/el "p" {}
                          "hello!"))))

(defn index
  [req]
  {:body    (ui-hello {})
   :status  200
   :headers {"Content-Type" (mime/default-mime-types "html")}})

(def routes
  `#{["/" :get index]})

(defonce state (atom nil))
(defn -main
  [& _]
  (swap! state
         (fn [st]
           (some-> st
                   http/stop)
           (-> {::http/type   :jetty
                ::http/port   8080
                ::http/join?  false
                ::http/routes #(route/expand-routes @#'routes)}
               http/default-interceptors
               http/dev-interceptors
               http/create-server
               http/start))))
