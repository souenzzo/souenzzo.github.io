(ns br.com.souenzzo.ds-http-test
  (:require [br.com.souenzzo.ds-http :as ds]
            [io.pedestal.test :refer [response-for]]
            [io.pedestal.http :as http]
            [midje.sweet :refer [fact =>]]
            [br.com.souenzzo.ds-http.testing :refer [str->is request->input-stream]]
            [clojure.test :refer [deftest is testing]]
            [io.pedestal.interceptor.chain :as chain]
            [clojure.string :as string]
            [io.pedestal.interceptor :as interceptor])
  (:import (java.io ByteArrayOutputStream ByteArrayInputStream)
           (java.net Socket)
           (java.nio.charset StandardCharsets)))

(set! *warn-on-reflection* true)


(deftest hello-pedestal
  (let [service-map (-> {::http/type           ds/http:type
                         ::http/chain-provider ds/http:chain-provider
                         ::http/port           8080
                         ::http/join?          false
                         ::http/routes         #{["/foo" :get (fn [request]
                                                                (prn :request)
                                                                {:status 202})
                                                  :route-name ::hello]}}
                        http/default-interceptors
                        http/create-server
                        http/start)]
    (try
      (fact
        (try
          (slurp "http://localhost:8080/foo")
          (catch Throwable ex
            {:fail (ex-message ex)}))
        => {})

      (finally
        (http/stop service-map)))))


(defn input-stream-join
  [separator coll]
  (ByteArrayInputStream. (.getBytes
                           (string/join
                             separator
                             coll)
                           StandardCharsets/UTF_8)))

(deftest hello-orig
  (let [*req (promise)
        service-fn (-> {::http/interceptors
                        (map interceptor/interceptor
                             [{:name  :a
                               :enter (fn [{:keys [request]
                                            :as   ctx}]
                                        (def _ctx ctx)
                                        (deliver *req (select-keys request
                                                                   [:server-port
                                                                    :server-name
                                                                    :query-string
                                                                    :scheme
                                                                    :request-method
                                                                    :uri
                                                                    :headers
                                                                    :remote-addr]))
                                        ctx)}])}
                       http/create-servlet
                       ::http/service-fn)]
    (response-for service-fn :get "http://foo.bar:8080/var?car=33#tar")
    (fact
      @*req
      => {:headers        {"content-length" "0"
                           "content-type"   ""}
          :query-string   "car=33"
          :remote-addr    "127.0.0.1"
          :request-method :get
          :scheme         :http
          :server-name    "foo.bar"
          :server-port    8080
          :uri            "/var"})))

(deftest hello-chain
  (let [baos (ByteArrayOutputStream.)
        *req (promise)
        client (proxy [Socket] []
                 (getInputStream []
                   (input-stream-join "\r\n" ["GET /var?car=33 HTTP/1.1"
                                              "Hello: world"
                                              ""
                                              "{}"]))
                 (getOutputStream []
                   baos))]
    (chain/execute
      {::ds/client client}
      (conj ds/base-interceptors
            (interceptor/interceptor {:name  ::a
                                      :enter (fn [{:keys [request]
                                                   :as   ctx}]
                                               (deliver *req (select-keys request
                                                                          [:server-port
                                                                           :server-name
                                                                           :query-string
                                                                           :scheme
                                                                           :request-method
                                                                           :uri
                                                                           :headers
                                                                           :remote-addr]))
                                               (assoc ctx
                                                 :response {:status  200
                                                            :headers {"Hello" "World!"}
                                                            :body    "ok!"}))})))
    (fact
      @*req
      => {:headers        {"hello" "world"}
          :query-string   "car=33"
          ; :remote-addr    "127.0.0.1"
          :request-method :get
          ; :scheme         :http
          ; :server-name    "foo.bar"
          ; :server-port    8080
          :uri            "/var"})
    (fact
      (string/split-lines (str baos))
      => ["HTTP/1.1 200 OK"
          "Hello: World!"
          ""
          "ok!"])))









