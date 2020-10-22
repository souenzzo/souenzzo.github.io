(ns br.com.souenzzo.choc
  "Classless Higher-Order Components"
  (:require [com.wsscode.pathom.connect :as pc]
            [edn-query-language.core :as eql]))

(pc/defresolver vs-table [{:keys [parser]
                           :as   env}
                          {::keys [display-properties join-key]}]
  {::pc/output [::vs-table]}
  ;; deve ter uma arvore de ID's/ENV/placeholder para parametrizar os parametros!!!
  (let [display-nodes (:children (eql/query->ast display-properties))
        query `[{~join-key ~display-properties}]
        result (parser env query)]
    {::vs-table [:table
                 [:thead
                  [:tr {}
                   (for [{:keys [params dispatch-key]} display-nodes]
                     [:th {}
                      [:a {:href "#"}
                       (::label params (name dispatch-key))]])]]
                 [:tbody
                  {}
                  (for [v (get result join-key)]
                    [:tr
                     {}
                     (for [{:keys [dispatch-key]} display-nodes]
                       [:td {} (get v dispatch-key)])])]]}))

(def register
  [vs-table])

