(ns br.com.souenzzo.hiete
  (:require [io.pedestal.http.route :as route]
            [hiccup2.core :as h]
            [io.pedestal.http.csrf :as csrf]
            [ring.util.mime-type :as mime])
  (:import (java.nio.charset StandardCharsets)
           (java.net URI)))

(def ^String utf-8 (str (StandardCharsets/UTF_8)))

(set! *warn-on-reflection* true)
(def ^:dynamic *route* nil)

(defn href
  [route-name & opts]
  #_(URI. (apply route/url-for route-name opts))
  (apply route/url-for route-name opts))

(defn mutation
  [{::csrf/keys [anti-forgery-token]} sym]
  {:method "POST"
   :action (href :conduit.api/mutation
                 :params {:sym                                  sym
                          (keyword csrf/anti-forgery-token-str) anti-forgery-token})})

(defn read-token
  [{:keys [query-params]}]
  (get query-params (keyword csrf/anti-forgery-token-str)))

(def render-hiccup
  {:name  ::render-hiccup
   :enter (fn [{:keys [bindings route]
                :as   ctx}]
            (assoc-in ctx [:bindings #'*route*] route))
   :leave (fn [{:keys [response]
                :as   ctx}]
            (if-let [body (:html response)]
              (-> ctx
                  (assoc-in [:response :body] (->> body
                                                   (h/html {:mode :html})
                                                   (str "<!DOCTYPE html>\n")))
                  (assoc-in [:response :headers "Content-Type"] (mime/default-mime-types "html")))
              ctx))})
