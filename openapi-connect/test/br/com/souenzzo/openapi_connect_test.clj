(ns br.com.souenzzo.openapi-connect-test
  (:require [clojure.test :refer [deftest]]
            [br.com.souenzzo.openapi-connect :as oc]
            [midje.sweet :refer [fact =>]]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom3.connect.operation :as pco]
            [clojure.data.json :as json]
            [com.wsscode.pathom3.interface.eql :as p.eql]
            [clojure.java.io :as io]
            [com.wsscode.pathom3.connect.indexes :as pci]
            [clojure.spec.test.alpha :as stest]))

(def openapi (json/read (io/reader (io/resource "realworld/api/swagger.json"))))

(deftest simple
  (stest/instrument)
  (let [resolver (-> {::oc/spec   openapi
                      ::oc/path   "/tags"
                      ::oc/method "get"
                      ::oc/ns     "gothinkster.realworld.conduit.api"}
                     oc/sync-resolve-fn
                     pco/resolver)

        index (pci/register resolver)]
    (fact
      (p.eql/process (with-meta index
                                {`oc/fetch (fn [_]
                                             {::oc/status 200
                                              ::oc/body   :ok})})
                     [:gothinkster.realworld.conduit.api/TagsResponse])
      => {:gothinkster.realworld.conduit.api/TagsResponse :ok})))

