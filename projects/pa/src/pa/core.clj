(ns pa.core
  (:require [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom3.interface.eql :as p.eql]
            [com.wsscode.pathom3.connect.indexes :as pci]
            [com.wsscode.pathom3.connect.runner :as pcr]
            [com.wsscode.pathom3.plugin :as p.plugin]
            [com.wsscode.pathom3.cache :as p.cache]
            [com.wsscode.pathom3.connect.operation :as pco]
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


(pco/defresolver scraper [{::keys [^HttpClient http-client] :as env} _]
  {::pco/output [::scraper]}
  (let [{::keys [^String url]} (pco/params env)
        req (.build (HttpRequest/newBuilder (URI/create url)))
        res (.send http-client
              req
              (HttpResponse$BodyHandlers/ofInputStream))
        body ^InputStream (.body res)]
    {::scraper {::body     body
                ::document (Jsoup/parse body (str StandardCharsets/UTF_8) url)
                ::headers  (into {} (.map (.headers res)))
                ::status   (.statusCode res)}}))

(pco/defresolver select [env {::keys [^Document document]}]
  {::pco/output [::select]}
  (let [{::keys [^String selector]} (pco/params env)
        v (string/join ""
            (for [^Element el (.select document selector)
                  txt (.textNodes el)]
              (str txt)))]
    {::select v}))

(defn sample-merge-attribute-wrapper
  [original]
  (fn [env out v]
    (if-let [as (-> v :params :pathom/as)]
      (let [dispath-key (-> v :dispatch-key)]
        (original env
          (into (empty out)
            (map (fn [[k v]]
                   [(if (= dispath-key k)
                      as
                      k)
                    v]))
            out)
          (assoc v
            :key as
            :dispatch-key as)))
      (original env out v))))
(p.plugin/defplugin protect-attributes-plugin
  {:com.wsscode.pathom3.format.eql/wrap-map-select-entry
   sample-merge-attribute-wrapper})

(defn process2
  [env tx]
  (let [env (merge env
              (pci/register
                (p.plugin/register protect-attributes-plugin)
                [select scraper])
              {::pcr/resolver-cache* nil})]
    (p.eql/process env tx)))