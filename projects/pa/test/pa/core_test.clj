(ns pa.core-test
  (:require [pa.core :as pa]
            [hiccup2.core :as h]
            [clojure.test :refer [deftest is]]
            [com.wsscode.pathom.connect.graphql2 :as pcg]
            [clojure.pprint :as pprint]
            [com.wsscode.pathom3.connect.runner :as pcr]
            [clojure.java.io :as io]
            [com.walmartlabs.lacinia.parser.schema :as lps]
            [com.wsscode.pathom3.connect.indexes :as pci]
            [com.wsscode.pathom3.plugin :as p.plugin]
            [com.wsscode.pathom3.interface.eql :as p.eql]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia :as lacinia]
            [clojure.string :as string])
  (:import (java.net.http HttpResponse HttpClient HttpHeaders HttpResponse$BodyHandlers HttpRequest)
           (java.util.function BiPredicate)
           (java.net URI)
           (org.jsoup Jsoup)
           (java.nio.charset StandardCharsets)
           (java.io InputStream)
           (org.jsoup.nodes Element Document)))
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

(defn mock-http-client
  [{::keys []}]
  (proxy [HttpClient] []
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
          200)))))

(deftest with-pathom
  (let []
    (is (-> (pa/process {::pa/http-client (mock-http-client {})}
              `[{(:scraper/g1 {:url "https://g1.globo.com"})
                 [(:select/title {:selector "head > title"})
                  (:select/description {:selector "body > div"})]}])
          (doto pprint/pprint)
          (= {:scraper/g1 {:select/title       "Hello"
                           :select/description "\nWorld"}})))))

(deftest with-pathom3
  (let [env (merge {::pa/http-client (mock-http-client {})}
              (pci/register
                (p.plugin/register pa/pathom-as-plugin)
                [pa/select pa/scraper])
              {::pcr/resolver-cache* nil})]
    (is (-> (p.eql/process env
              `[{(::pa/scraper {::pa/url   "https://g1.globo.com"
                                :pathom/as :g1})
                 [(::pa/select {::pa/selector "head > title"
                                :pathom/as    :title})
                  (::pa/select {::pa/selector "body > div"
                                :pathom/as    :description})]}])
          (doto pprint/pprint)
          (= {:scraper/g1 {:select/title       "Hello"
                           :select/description "\nWorld"}})))))


(deftest with-lacinia
  (let [schema {:objects {:document {:fields {:selectString {:type    'String
                                                             :resolve (fn [_ {:keys [^String cssQuery]} {::keys [^Document document]}]
                                                                        (string/join ""
                                                                          (for [^Element el (.select document cssQuery)
                                                                                txt (.textNodes el)]
                                                                            (str txt))))
                                                             :args    {:cssQuery {:type 'String}}}}}}
                :queries {:scraper {:type    :document
                                    :args    {:url {:type 'String}}
                                    :resolve (fn [{::keys [^HttpClient http-client]} {:keys [url]} _]
                                               (let [req (.build (HttpRequest/newBuilder (URI/create url)))
                                                     res (.send http-client
                                                           req
                                                           (HttpResponse$BodyHandlers/ofInputStream))
                                                     body ^InputStream (.body res)]
                                                 {::body     body
                                                  ::document (Jsoup/parse body (str StandardCharsets/UTF_8) (str url))
                                                  ::headers  (into {} (.map (.headers res)))
                                                  ::status   (.statusCode res)}))}}}

        schema (schema/compile schema)
        query "{
                 g1: scraper(url: \"https://g1.globo.com\") {
                   title: selectString(cssQuery: \"head > title\"),
                   description: selectString(cssQuery: \"body > div\")
                 }
               }"
        variables {}
        context {::http-client (mock-http-client {})}
        options {}]
    (is (-> (lacinia/execute schema query variables context options)
          (doto pprint/pprint)
          (= {:data {:g1 {:title       "Hello"
                          :description "\nWorld"}}})))))
