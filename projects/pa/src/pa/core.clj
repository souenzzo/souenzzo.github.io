(ns pa.core
  (:require [com.wsscode.pathom.core :as p]
            [clj-xpath.core :as xp])
  (:import (java.net URI)
           (java.net.http HttpClient HttpRequest HttpResponse$BodyHandlers)))

"
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
`[{(:scraper/g1 {:url "https://g1.globo.com"})
   [(:select/title {:xpath "tr > div > h1"})
    {(:select/comments {:xpath "tr > div > div:nth-child(5) > div"})
     [(:select/name {:xpath "h1"})
      (:select/msg {:xpath "h1"})
      {(:scraper/twitter {:params {:id "h1"}
                          :url    "https://twitter.com/{id}"})
       [(:select/last-tweet {:xpath "div > main > div"})]}]}]}]
(set! *warn-on-reflection* true)

(defn scraper-reader
  [{:keys  [ast]
    ::keys [^HttpClient http-client]
    :as    env}]
  (let [{:keys [dispatch-key params]} ast
        impl (some-> dispatch-key namespace keyword)]
    (if (contains? #{:scraper} impl)
      (let [req (.build (HttpRequest/newBuilder
                          (URI/create (str (:url params)))))
            res (.send http-client
                  req
                  (HttpResponse$BodyHandlers/ofString))]
        (p/join {:body    (.body res)
                 :headers (into {} (.map (.headers res)))
                 :status  (.statusCode res)}
          env))
      ::p/continue)))

(defn select-reader
  [{:keys    [ast]
    ::p/keys [entity]}]
  (let [{:keys [dispatch-key params]} ast
        impl (some-> dispatch-key namespace keyword)]
    (if (contains? #{:select} impl)
      (let [{:keys [body]} @entity
            {:keys [xpath]} params]
        (xp/$x:text xpath body))
      ::p/continue)))
(def process
  (p/parser {::p/env {::p/reader [scraper-reader
                                  select-reader]}}))
