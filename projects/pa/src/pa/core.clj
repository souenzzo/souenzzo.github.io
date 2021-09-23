(ns pa.core
  (:require [com.wsscode.pathom.core :as p]
            [clojure.string :as string])
  (:import (java.io InputStream)
           (java.net URI)
           (java.net.http HttpClient HttpRequest HttpResponse$BodyHandlers)
           (java.nio.charset StandardCharsets)
           (org.jsoup Jsoup)
           (org.jsoup.nodes Element Document)))

(set! *warn-on-reflection* true)

(defn scraper-reader
  [{:keys  [ast]
    ::keys [^HttpClient http-client]
    :as    env}]
  (let [{:keys [dispatch-key params]} ast
        impl (some-> dispatch-key namespace keyword)]
    (if (contains? #{:scraper} impl)
      (let [url (str (:url params))
            req (.build (HttpRequest/newBuilder (URI/create url)))
            res (.send http-client
                  req
                  (HttpResponse$BodyHandlers/ofInputStream))
            body ^InputStream (.body res)]
        (p/join (assoc params
                  :body body
                  :document (Jsoup/parse body (str StandardCharsets/UTF_8) url)
                  :headers (into {} (.map (.headers res)))
                  :status (.statusCode res))
          env))
      ::p/continue)))

(defn select-reader
  [{:keys    [ast]
    ::p/keys [entity]}]
  (let [{:keys [dispatch-key params]} ast
        impl (some-> dispatch-key namespace keyword)]
    (if (contains? #{:select} impl)
      (let [{:keys [^Document document]} @entity
            {:keys [^String selector]} params]
        (string/join ""
          (for [^Element el (.select document selector)
                txt (.textNodes el)]
            (str txt))))
      ::p/continue)))

(def process
  (p/parser {::p/env {::p/reader [scraper-reader
                                  select-reader]}}))
