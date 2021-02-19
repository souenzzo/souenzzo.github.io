(ns br.com.souenzzo.conduit.http-test
  (:require [clojure.test :refer [deftest]]
            [io.pedestal.http :as http]
            [io.pedestal.test :refer [response-for]]
            [io.pedestal.http.route :as route]
            [clojure.string :as string]
            [midje.sweet :refer [fact =>]])
  (:import (java.net.http HttpResponse HttpClient HttpRequest HttpResponse$BodyHandlers)
           (java.net URI)))

(set! *warn-on-reflection* true)

(defn http:create-client
  [service-map]
  (let [{::http/keys [service-fn servlet]} (-> service-map
                                               http/create-servlet)]
    #_(def __servlet servlet)
    (proxy [HttpClient] []
      (send [^HttpRequest request body-hanlder]
        (let [url (str (.uri request))
              verb (keyword (string/lower-case (str (.method request))))
              {:keys [body status]} (response-for service-fn verb url)]
          (reify HttpResponse
            (statusCode [this]
              status)
            (request [this]
              request)))))))


(deftest http-clients-loop
  (let [*http-client (promise)
        proxy-routes (-> #{{:app-name :app-proxy
                            :host     "proxy.localhost"}
                           ["/hello" :get (fn [_]
                                            (let [url (route/url-for :app/hello
                                                                     :scheme :http
                                                                     :port 8080
                                                                     :app-name :app)
                                                  _ (prn [:url url])
                                                  request (-> (HttpRequest/newBuilder)
                                                              (.uri (URI/create (str "http:" url)))
                                                              (.build))
                                                  response (.send ^HttpClient @*http-client request
                                                                  (HttpResponse$BodyHandlers/ofString))]
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
        ^HttpClient client (-> {::http/routes routes}
                               http/default-interceptors
                               http:create-client
                               (doto (->> (deliver *http-client))))
        url-for (route/url-for-routes routes)
        url (url-for :proxy/hello
                     :scheme :http
                     :port 8080
                     :app-name :app-proxy)]
    (fact
      "url"
      url
      => "http://proxy.localhost:8080/hello")
    (fact
      "client"
      (-> client
          (.send (-> (HttpRequest/newBuilder)
                     (.uri (URI/create url))
                     (.build))
                 (HttpResponse$BodyHandlers/ofString))
          (.statusCode))
      => 202)))

