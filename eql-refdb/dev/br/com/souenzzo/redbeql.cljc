(ns br.com.souenzzo.redbeql
  (:require [br.com.souenzzo.eql-refdb :as refdb]
            [com.wsscode.pathom.core :as p]
            [re-frame.core :as rf]
            [clojure.core.async :as async]
            [com.wsscode.pathom.connect :as pc]
            [clojure.spec.alpha :as s]
            [edn-query-language.core :as eql]))


(defn on-result
  [db [_ tx tree]]
  (prn [:db db :tx tx :tree tree])
  (refdb/merge-tree
    db
    tree
    (eql/query->ast tx)))


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

(defn eql
  [{::keys [on-result]
    :as    env}]
  (fn [tx]
    (let [tree (parser env tx)]
      (async/go
        (rf/dispatch [on-result tx (async/<! tree)])))))

(s/fdef eql
        :args (s/cat :env (s/keys :req [::on-result])))
