(ns br.com.souenzzo.conduit.ssr-test
  (:require [clojure.test :refer [deftest]]
            [br.com.souenzzo.conduit.ssr :as conduit]
            [midje.sweet :refer [fact =>]]
            [io.pedestal.test :refer [response-for]]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]))

(deftest hello
  (let [service-fn (-> conduit/service
                       http/create-servlet
                       ::http/service-fn)
        url-for (route/url-for-routes (route/expand-routes conduit/routes))]
    (fact
      (-> (response-for service-fn :get
                        (url-for ::conduit/index))
          :status)
      => 200)))
