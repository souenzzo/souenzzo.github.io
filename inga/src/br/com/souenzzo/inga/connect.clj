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

(def parser
  (p/parser {::p/plugins [(pc/connect-plugin)]
             ::p/env     {::p/reader               [p/map-reader
                                                    pc/reader2
                                                    pc/open-ident-reader
                                                    p/env-placeholder-reader]
                          ::p/placeholder-prefixes #{">"}}}))

(s/fdef parser
        :args (s/cat :env (s/keys :req [::pc/indexes])
                     :tx ::eql/query)
        :ret map?)

(defn hparser
  [env v]
  (cond
    (custom-form? v) (hparser env (get (parser env [(seq v)])
                                       (first v)))
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
