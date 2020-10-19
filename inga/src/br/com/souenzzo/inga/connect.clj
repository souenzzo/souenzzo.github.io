(ns br.com.souenzzo.inga.connect
  (:require [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [clojure.spec.alpha :as s]
            [edn-query-language.core :as eql]))


(defn custom-form?
  [v]
  (and (coll? v)
       (qualified-keyword? (first v))))


(defn tagged-form?
  [v]
  (and (coll? v)
       (keyword? (first v))))

(defn query
  [hiccup]
  (let [attrs? (map? (second hiccup))]
    (if (custom-form? hiccup)
      (let [child (mapcat query
                          (drop (if attrs? 2 1)
                                hiccup))
            params (when attrs?
                     (second hiccup))
            prop (if (empty? params)
                   (first hiccup)
                   (list (first hiccup) (second hiccup)))]
        [(if (empty? child)
           prop
           {prop (vec  child)})])
      (into []
            (mapcat query)
            (if attrs?
              (drop 2 hiccup)
              (drop 1 hiccup))))))

(defn env-placeholder-reader
  [{::p/keys [placeholder-prefixes] :as env}]
  (assert placeholder-prefixes "To use env-placeholder-reader please add ::p/placeholder-prefixes to your environment.")
  (if (p/placeholder-key? env (-> env :ast :dispatch-key))
    (let [params (-> env :ast :params)]
      (p/swap-entity! env (fnil into {})
                      params)
      (p/join env))
    ::p/continue))


(def parser
  (p/parser {::p/plugins [(pc/connect-plugin)]
             ::p/env     {::p/reader               [p/map-reader
                                                    pc/reader2
                                                    pc/open-ident-reader
                                                    env-placeholder-reader]
                          ::p/placeholder-prefixes #{">"}}}))

(s/fdef parser
        :args (s/cat :env (s/keys :req [::pc/indexes])
                     :tx ::eql/query)
        :ret map?)

(defn hparser
  [env v]
  (cond
    (custom-form? v) (let [[k inputs] v
                           tx `[{(:>/env ~(into {} inputs))
                                 [~k]}]
                           result (parser env tx)]
                       (hparser env (get-in result
                                            [:>/env k])))
    (tagged-form? v) (let [opts (second v)
                           body (if (map? opts)
                                  (drop 2 v)
                                  (drop 1 v))
                           opts (if (map? opts)
                                  opts
                                  {})]
                       (into [(first v) opts]
                             (map (partial hparser env))
                             body))
    :else v))
