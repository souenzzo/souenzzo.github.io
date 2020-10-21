(ns br.com.souenzzo.inga.connect
  (:require [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [clojure.spec.alpha :as s]
            [edn-query-language.core :as eql]))

(declare xf-ast)

(defn xf-ast-tag
  [[tag x & xs]]
  (let [attrs? (map? x)
        append? (and x (not attrs?))
        children? (or append?
                      xs)
        children (vec (concat (when append?
                                [x])
                              xs))]
    [(cond-> {:type :element
              :tag  tag}
             attrs? (assoc :attrs x)
             children? (assoc :children (into []
                                              xf-ast children)))]))

(def xf-ast
  (mapcat (fn [el]
            (cond
              (and (coll? el)
                   (keyword? (first el)))
              (xf-ast-tag el)
              (coll? el) [{:type     :fragment
                           :children (into [] xf-ast el)}]
              :else [{:type  :literal
                      :value el}]))))

(defn ->ast
  [hiccup]
  (first
    (sequence
      xf-ast
      [hiccup])))

(defn ->html
  [{:keys [tag children value attrs]}]
  (cond
    (and tag attrs children) (into [tag attrs]
                                   (map ->html)
                                   children)
    (and tag children) (into [tag]
                             (map ->html)
                             children)
    tag [tag]
    children (map ->html children)
    :else value))

(defn custom-form?
  [v]
  (and (coll? v)
       (qualified-keyword? (first v))))


(defn tagged-form?
  [v]
  (and (coll? v)
       (keyword? (first v))))

(defn ->eql
  [{:keys [type tag children attrs]}]
  {:type     :root
   :children (if (qualified-keyword? tag)
               [(cond-> {:type         (if children
                                         :join
                                         :prop)
                         :key          tag
                         :dispatch-key tag}
                        attrs (assoc :params attrs)
                        children (assoc :children (into []
                                                        (comp (map ->eql)
                                                              (mapcat :children))
                                                        children)))]
               (into []
                     (comp (map ->eql)
                           (mapcat :children))
                     children))})


(defn xf-place
  [data]
  (map (fn [{:keys [tag children]
             :as   node}]
         (cond
           (contains? data tag) (->ast (get data tag))
           children (assoc node
                      :children (into []
                                      (xf-place data)
                                      children))
           :else node))))


(defn place
  [ast data]
  (first (sequence (xf-place data)
                   [ast])))


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
