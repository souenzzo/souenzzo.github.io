(ns br.com.souenzzo.missoshiro
  (:require [clojure.string :as string]
            [io.pedestal.test :refer [response-for]]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor])
  (:import (java.net.http HttpResponse$BodyHandlers
                          HttpResponse
                          HttpRequest
                          HttpClient)
           (java.net URI)))

(set! *warn-on-reflection* true)

(defn create-client
  [service-map]
  (let [*client (promise)
        {::http/keys [service-fn routes]} (-> service-map
                                              (update ::http/interceptors
                                                      (partial into [(interceptor/interceptor {:name  ::client
                                                                                               :enter (fn [ctx]
                                                                                                        (let [client @*client]
                                                                                                          (assoc-in ctx [:request ::client] client)))})]))
                                              http/create-servlet)
        client (proxy [HttpClient] []
                 (send [^HttpRequest request body-hanlder]
                   (let [url (str (.uri request))
                         verb (keyword (string/lower-case (str (.method request))))
                         {:keys [body status]} (response-for service-fn verb url)]
                     (reify HttpResponse
                       (statusCode [this]
                         status)
                       (request [this]
                         request)))))]
    (deliver *client client)
    (assoc service-map
      ::url-for (route/url-for-routes routes :scheme :http)
      ::client client)))

(defn request
  [{::keys [^HttpClient client url-for]
    :or    {url-for route/url-for}} route-name & opts]
  (let [url (apply url-for route-name opts)
        request (-> (HttpRequest/newBuilder)
                    (.uri (URI/create  url))
                    (.build))]
    (.send client request
           (HttpResponse$BodyHandlers/ofString))))
