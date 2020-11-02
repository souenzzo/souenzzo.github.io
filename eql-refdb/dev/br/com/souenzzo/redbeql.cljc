(ns br.com.souenzzo.redbeql
  (:require [br.com.souenzzo.eql-refdb :as refdb]
            [com.wsscode.pathom.core :as p]
            [re-frame.core :as rf]
            [clojure.core.async :as async]
            [com.wsscode.pathom.connect :as pc]
            [clojure.spec.alpha :as s]))


(defn on-result
  [db [_ tx tree]]
  (refdb/tree->db {::refdb/db   db
                   ::refdb/tree tree
                   ::refdb/tx   tx}))


(defn env-placeholder-reader-v2
  [{:keys [ast] :as env}]
  (if (p/placeholder-key? env (:dispatch-key ast))
    (let [params (:params ast)]
      (p/swap-entity! env (fn [entity]
                            (merge entity params)))
      (p/join env))
    ::continue))

(s/fdef env-placeholder-reader-v2
        :args (s/cat :env (s/keys)))

(s/fdef on-result
        :args (s/cat :db map
                     :values (s/tuple
                               :self keyword?
                               :tx vector?
                               :tree map?)))

(def parser
  (p/parallel-parser {::p/plugins [(pc/connect-plugin)]
                      ::p/mutate  pc/mutate-async
                      ::p/env     {::p/reader               [p/map-reader
                                                             pc/reader2
                                                             pc/open-ident-reader
                                                             env-placeholder-reader-v2]
                                   ::p/placeholder-prefixes #{">"}}}))

(defn select-sub
  [db [_ query]]
  (let [tree (refdb/db->tree {::refdb/db    db
                              ::refdb/query query})]
    tree))
(defn parser-event-fx [{::keys [parser-fx]}]
  (fn [_ [_ tx]]
    {parser-fx tx}))

(defn parser-fx
  [{::keys [on-result]
    :as    env}]
  (fn [tx]
    (let [tree (parser env tx)]
      (async/go
        (rf/dispatch [on-result tx (async/<! tree)])))))

(s/fdef parser-fx
        :args (s/cat :env (s/keys :req [::on-result])))
