(ns br.com.souenzzo.inga.dev-server
  (:require [br.com.souenzzo.inga.ring :as ir]
            [clojure.pprint :as pp]
            [com.wsscode.pathom.connect :as pc]
            [io.pedestal.http :as http]
            [io.pedestal.http.csrf :as csrf]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [ring.util.mime-type :as mime])
  (:import (java.nio.charset StandardCharsets)))

(pc/defresolver form [{::csrf/keys [anti-forgery-token]} {::keys [action inputs]}]
  {::form [:form
           {:method "POST"
            :action action}
           [:input {:hidden true
                    :value  anti-forgery-token
                    :name   csrf/anti-forgery-token-str}]
           (for [v inputs]
             [:label
              v
              [:input {:name v}]])
           [:input {:type  "submit"
                    :value action}]]})

(pc/defresolver todo-list [{::keys [todos]} _]
  {::todo-list [:div
                [:ul
                 (for [v @todos]
                   [:li v])]
                [:>/env1 {::action "/app/new-todo"
                          ::inputs ["app/todo"]}
                 [::form]]]})


(pc/defmutation new-todo [{::keys [todos]} {:keys [app/todo]}]
  {::pc/sym 'app/new-todo}
  (swap! todos conj todo)
  {})


(pc/defresolver current-value [{::keys [counter]} _]
  {::current-value @counter})

(pc/defresolver counter-display [_ {::keys [current-value]}]
  {::counter-display [:p (str current-value)]})

(pc/defresolver counter-controls [{::csrf/keys [anti-forgery-token]} _]
  {::counter-controls [:>/env2
                       {::action "/app/inc"
                        ::inputs []}
                       [::form]]})

(pc/defmutation increment [{::keys [counter]} _]
  {::pc/sym 'app/inc}
  (swap! counter inc)
  {})

(pc/defmutation mutate [{:keys [parser path-params form-params]
                         :as   env} params]
  {::pc/sym    `ir/mutate
   ::pc/output [::ir/body
                ::ir/status
                ::ir/headers]}
  (let [tx `[{(~(symbol (:mutation path-params)) ~(into {} form-params))
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
                   new-todo
                   todo-list
                   form
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
               [::counter-controls {}]
               [::todo-list {}]]]
   :headers  {"Content-Security-Policy" ""
              "Content-Type"            (mime/default-mime-types "html")}
   :status   200})

(defonce counter (atom 0))
(defonce todos (atom []))
(def connect (ir/interceptor {::counter    counter
                              ::todos      todos
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
                ::http/enable-csrf           {}
                ::http/type                  :jetty}
               http/default-interceptors
               http/dev-interceptors
               http/create-server
               http/start))))
