(ns pa.core-test
  (:require [pa.core :as pa]
            [hiccup2.core :as h]
            [clojure.test :refer [deftest is]]
            [clojure.pprint :as pprint])
  (:import (java.net.http HttpResponse HttpClient HttpHeaders)
           (java.util.function BiPredicate)))


(deftest hello
  (let []
    (is (-> (pa/process {::pa/http-client (proxy [HttpClient] []
                                            (send [req res]
                                              (reify HttpResponse
                                                (body [this]
                                                  (->> [:html
                                                        [:head
                                                         [:title "Hello"]]
                                                        [:body
                                                         [:div "World"]]]
                                                    (h/html {:mode :html})
                                                    str))
                                                (headers [this] (HttpHeaders/of {}
                                                                  (reify BiPredicate
                                                                    (test [this a b]
                                                                      true))))
                                                (statusCode [this]
                                                  200))))}
              `[{(:scraper/g1 {:url "https://g1.globo.com"})
                 [(:select/title {:xpath "./html/head/title"})]}])
          (doto pprint/pprint)
          (= {:scraper/g1 {:select/title "Hello"}})))))
