(ns br.com.souenzzo.conduit.http-test
  (:require [clojure.test :refer [deftest]]
            [io.pedestal.http :as http]
            [io.pedestal.test :refer [response-for]]
            [io.pedestal.http.route :as route]
            [br.com.souenzzo.missoshiro :as misso]
            [midje.sweet :refer [fact =>]]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.interceptor.chain :as chain]
            [io.pedestal.http.route.map-tree :as map-tree]
            [io.pedestal.http.route.router :as router])
  (:import (java.net.http HttpClient
                          HttpRequest
                          HttpResponse$BodyHandlers)
           (java.net URI)))

(set! *warn-on-reflection* true)

(deftest http-clients-loop
  (let [proxy-routes (-> #{{:app-name :app-proxy
                            :host     "proxy.localhost"}
                           ["/hello" :get (fn [req]
                                            (let [response (misso/request req
                                                                          :app/hello
                                                                          :app-name :app
                                                                          :scheme :http
                                                                          :scheme :http
                                                                          :port 8080)]
                                              (prn :proxy)
                                              {:status (.statusCode response)}))
                            :route-name :proxy/hello]}
                         route/expand-routes)
        app-routes (-> #{{:app-name :app
                          :host     "app.localhost"}
                         ["/hello" :get (fn [_]
                                          (prn :app)
                                          {:status 202})
                          :route-name :app/hello]}
                       route/expand-routes)
        routes (concat app-routes
                       proxy-routes)
        app (-> {::http/routes routes}
                http/default-interceptors
                misso/create-client)]
    (fact
      "client"
      (-> (misso/request app
                         :proxy/hello
                         :scheme :http
                         :port 8080
                         :app-name :app-proxy)
          (.statusCode))
      => 202)))

(deftest custom-http-router
  (let [router (map-tree/router [{:path          "/hello"
                                  ::interceptors (->> [{:name  ::b
                                                        :enter (fn [ctx]
                                                                 (assoc ctx :response {:status 303}))}]
                                                      (map interceptor/interceptor))}])
        interceptors (->> [{:name  ::a
                            :enter (fn [{:keys [request]
                                         :as   ctx}]

                                     (let [{::keys [interceptors]
                                            :as    route} (router/find-route router request)]
                                       (-> ctx
                                           (assoc :request (merge request route))
                                           (chain/enqueue interceptors))))}]
                          (map interceptor/interceptor))
        service-fn (-> {::http/interceptors interceptors}
                       http/create-servlet
                       ::http/service-fn)]
    (fact
      (response-for service-fn :get "/hello")
      => {})
    (fact

      => {})))
