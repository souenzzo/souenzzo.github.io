(ns br.com.souenzzo.conduit.ssr
  (:require [io.pedestal.http :as http]
            [hiccup2.core :as h]
            [hiccup.util :as hu]
            [ring.util.mime-type :as mime]
            [io.pedestal.http.route :as route]
            [clojure.pprint :as pp])
  (:import (java.net URI)))

(defn index-a
  [req]
  [:html
   [:head]
   [:body
    [:div "ok!"]
    [:div
     [:a {:href (URI. (route/url-for ::index-b))}
      "bb"]]
    [:a {:href (route/url-for ::index-b)}
     "b"]]])


(defn index-b
  [req]
  [:html
   [:head]
   [:body
    [:div "ok!"]
    [:div
     [:a {:href (URI. (route/url-for ::index-a))}
      "aa"]]
    [:a {:href (route/url-for ::index-a)}
     "a"]]])


(def hiccup->response
  {:name  ::hiccup
   :leave (fn [{:keys [response]
                :as   ctx}]
            (if (and (coll? response)
                     (not (map? response)))
              (assoc ctx :response {:body   response
                                    :status 200})
              ctx))})

(def render-hiccup
  {:name  ::render-hiccup
   :leave (fn [{:keys [response]
                :as   ctx}]
            (if-let [body (:body response)]
              (try
                (let [url (some->> (get-in ctx [:request :headers "referer"])
                                   (URI.))]
                  (pp/pprint (bean url))
                  (-> ctx
                      (assoc-in [:response :body] (binding [hu/*base-url* (get-in ctx [:request :headers "referer"])]
                                                    (prn (-> ctx :request :headers))
                                                    (->> body
                                                         (h/html {:mode :html})
                                                         (str "<!DOCTYPE html>\n"))))
                      (assoc-in [:response :headers "Content-Type"] (mime/default-mime-types "html"))))
                (catch Throwable ex                         ;; not a valid html body
                  ctx))
              ctx))})

(def routes
  `#{["/a" :get [render-hiccup hiccup->response index-a]]
     ["/b" :get [render-hiccup hiccup->response index-b]]})

(def service
  (-> {::http/join?  false
       ::http/port   8080
       ::http/routes routes
       ::http/type   :jetty}
      http/default-interceptors))


(defonce state (atom nil))

(defn -main
  [& _]
  (swap! state
         (fn [st]
           (some-> st http/stop)
           (-> service
               http/create-server
               http/start))))