(ns pa.core-test
  (:require [pa.core :as pa]
            [hiccup2.core :as h]
            [clojure.test :refer [deftest is]]
            [com.wsscode.pathom.connect.graphql2 :as pcg]
            [clojure.pprint :as pprint]
            [clojure.java.io :as io])
  (:import (java.net.http HttpResponse HttpClient HttpHeaders)
           (java.util.function BiPredicate)))
;; https://docs.oracle.com/en/java/javase/17/docs/api/java.xml/javax/xml/xpath/package-summary.html

(set! *warn-on-reflection* true)
#_"
query {
  g1:scraper(url: \"https://g1.globo.com\") {
    title:    selectString(\"tr > div > h1\")
    comments: selectString(\"tr > div > div:nth-child(5) > div\") {
      name: selectString(\"h1\")
      msg: selectString(\"div\")
      twitter: scraper(url:\"https://twitter.com/%s\", selectParams: [\"main > div.twitter\"]) {
        lastTweet: selectString(\"div > main > div\")
      }
    }
  }
}
"
#_`[{(:scraper/g1 {:url "https://g1.globo.com"})
     [(:select/title {:xpath "tr > div > h1"})
      {(:select/comments {:xpath "tr > div > div:nth-child(5) > div"})
       [(:select/name {:xpath "h1"})
        (:select/msg {:xpath "h1"})
        {(:scraper/twitter {:params {:id "h1"}
                            :url    "https://twitter.com/{id}"})
         [(:select/last-tweet {:xpath "div > main > div"})]}]}]}]

(comment
  ;; EQL <> GraphQL equivalence:
  (pcg/query->graphql `[{(:scraper/g1 {:url "https://g1.globo.com"})
                         [(:select/title {:xpath "tr > div > h1"})]}]
    {})
  => "query {
        g1(url: \"https:\\/\\/g1.globo.com\") {
          title(xpath: \"tr > div > h1\")
        }
      }
")

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
                                                    str
                                                    .getBytes
                                                    io/input-stream))
                                                (headers [this] (HttpHeaders/of {}
                                                                  (reify BiPredicate
                                                                    (test [this a b]
                                                                      true))))
                                                (statusCode [this]
                                                  200))))}
              `[{(:scraper/g1 {:url "https://g1.globo.com"})
                 [(:select/title {:selector "head > title"})
                  (:select/description {:selector "body > div"})]}])
          (doto pprint/pprint)
          (= {:scraper/g1 {:select/title       "Hello"
                           :select/description "\nWorld"}})))))
