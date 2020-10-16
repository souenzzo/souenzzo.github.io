(ns br.com.souenzzo.inga.dev-server
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [ring.util.mime-type :as mime]
            [br.com.souenzzo.inga.ring :as ir]
            [com.wsscode.pathom.connect :as pc]
            [io.pedestal.interceptor :as interceptor]
            [clojure.pprint :as pp])
  (:import (java.nio.charset StandardCharsets)))


(pc/defresolver current-value [{::keys [counter]} _]
  {::current-value @counter})

(pc/defresolver counter-display [_ {::keys [current-value]}]
  {::counter-display [:p (str current-value)]})

(pc/defresolver counter-controls [_ _]
  {::counter-controls [:form
                       {:method "POST"
                        :action "/app/inc"}
                       [:input {:type  "submit"
                                :value "+"}]]})

(pc/defmutation increment [{::keys [counter]} _]
  {::pc/sym 'app/inc}
  (swap! counter inc)
  {})

(pc/defmutation mutate [{:keys [parser path-params]
                         :as   env} params]
  {::pc/sym    `ir/mutate
   ::pc/output [::ir/body
                ::ir/status
                ::ir/headers]}
  (let [tx `[{(~(symbol (:mutation path-params)) ~{})
              []}]
        result (parser env tx)]
    {::ir/body    nil
     ::ir/status  303
     ::ir/headers {"Location" (-> env :headers (get "referer" "/"))}}))

(def indexes
  (pc/register {} [counter-display
                   current-value
                   increment
                   mutate
                   counter-controls]))

(defn index
  [req]
  {::ir/body [:html
              [:head
               [:meta {:charset (str StandardCharsets/UTF_8)}]
               [:link {:rel "icon" :href "data:"}]
               [:title "ingá!"]]
              [:body
               [::counter-display {}]
               [::counter-controls {}]]]
   :headers  {"Content-Security-Policy" ""
              "Content-Type"            (mime/default-mime-types "html")}
   :status   200})

(defonce counter (atom 0))
(def connect (ir/interceptor {::counter    counter
                              ::pc/indexes indexes}))

(def routes
  `#{["/" :get [connect index]]
     ["/*mutation" :post [connect ir/mutate]]})

(defonce state (atom nil))

(def not-found-interceptor
  (interceptor/interceptor
    {:name  ::not-found
     :leave (fn [{:keys [response request] :as ctx}]
              (if (http/response? response)
                ctx
                (assoc ctx :response
                           {:body    (with-out-str (pp/pprint request))
                            :headers {"Content-Type" (mime/default-mime-types "txt")}
                            :status  404})))}))

(defn -main
  [& _]
  (swap! state
         (fn [st]
           (some-> st http/stop)
           (-> {::http/routes                (fn []
                                               (route/expand-routes @(requiring-resolve `routes)))
                ::http/port                  8080
                ::http/not-found-interceptor not-found-interceptor
                ::http/join?                 false
                ::http/type                  :jetty}
               http/default-interceptors
               http/dev-interceptors
               http/create-server
               http/start))))
