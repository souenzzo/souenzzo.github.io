(ns br.com.souenzzo.redbeql
  (:require [br.com.souenzzo.eql-refdb :as refdb]
            [com.wsscode.pathom.core :as p]
            [re-frame.core :as rf]
            [clojure.core.async :as async]
            [com.wsscode.pathom.connect :as pc]
            [clojure.spec.alpha :as s]))


(defn on-result
  [db [_ tx result]]
  (let [db-after (refdb/tree->db
                   {::refdb/db     db
                    ::refdb/tx     tx
                    ::refdb/result result})]
    (prn {:tx tx :result result :db-before db :db-after db-after})
    db-after))


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
                               :result map?)))

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
    (let [result (parser env tx)]
      (async/go
        (rf/dispatch [on-result tx (async/<! result)])))))

(s/fdef eql
        :args (s/cat :env (s/keys :req [::on-result])))
