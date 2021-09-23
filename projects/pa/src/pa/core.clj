(ns pa.core
  (:require [com.wsscode.pathom.core :as p]
            [hiccup2.core :as h]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (java.net URI)
           (java.net.http HttpClient HttpRequest HttpResponse$BodyHandlers)
           (org.jsoup Jsoup)
           (java.nio.charset StandardCharsets)
           (org.jsoup.nodes Element Document)
           (java.io InputStream)))

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

(defn jsoup
  []
  (let [document (Jsoup/parse (->> [:html
                                    [:head
                                     [:title "Hello"]]
                                    [:body
                                     [:div "World"]]]
                                (h/html {:mode :html})
                                str
                                .getBytes
                                io/input-stream)
                   (str StandardCharsets/UTF_8)
                   "http://localhost")
        node (string/join ""
               (for [^Element el (.select document
                                   "title")
                     txt (.textNodes el)]
                 (str txt)))]
    node))

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
