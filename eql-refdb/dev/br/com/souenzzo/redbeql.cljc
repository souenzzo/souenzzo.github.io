(ns br.com.souenzzo.redbeql
  (:require [edn-query-language.core :as eql]
            [br.com.souenzzo.eql-refdb :as refdb]
            [com.wsscode.pathom.core :as p]
            [re-frame.core :as rf]
            [clojure.core.async :as async]
            [com.wsscode.pathom.connect :as pc]))


(defn on-result
  [db [_ value query]]
  (refdb/tree->db
    {::refdb/db    db
     ::refdb/value value
     ::refdb/query query}))

(defn eql
  [{::keys [on-result] :as env}]
  (let [env (merge {::p/reader               [p/map-reader
                                              pc/reader2
                                              pc/open-ident-reader
                                              p/env-placeholder-reader]
                    ::p/placeholder-prefixes #{">"}
                    ::p/plugins              [(pc/connect-plugin env)]
                    ::p/mutate               pc/mutate-async}
                   env)
        parser (p/parallel-parser env)]
    (fn [tx]
      (let [result (parser env tx)]
        (async/go
          (rf/dispatch [on-result (async/<! result) tx]))))))
