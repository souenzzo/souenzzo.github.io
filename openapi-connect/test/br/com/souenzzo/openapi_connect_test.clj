(ns br.com.souenzzo.openapi-connect-test
  (:require [clojure.test :refer [deftest]]
            [br.com.souenzzo.openapi-connect :as oc]
            [midje.sweet :refer [fact =>]]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom3.connect.operation :as pco]
            [clojure.data.json :as json]
            [clojure.java.io :as io]))

(def openapi (json/read (io/reader (io/resource "realworld/api/swagger.json"))))

(deftest simple
  (let [resolver (oc/sync-resolve-fn
                   {::oc/spec   openapi
                    ::oc/path   "/tags"
                    ::oc/method "get"
                    ::oc/ns     "gothinkster.realworld.conduit.api"})]
    (fact
      (resolver {} {})
      => {})))

