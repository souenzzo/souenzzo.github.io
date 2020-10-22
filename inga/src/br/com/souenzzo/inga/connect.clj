(ns br.com.souenzzo.inga.connect
  (:require [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [clojure.spec.alpha :as s]
            [edn-query-language.core :as eql]))

(def xf-ast
  (letfn [(xf-ast-tag [[tag x & xs]]
            (let [attrs? (map? x)
                  append? (and x (not attrs?))
                  children? (or append?
                                xs)
                  children (vec (concat (when append?
                                          [x])
                                        xs))]
              (cond-> {:type :element
                       :tag  tag}
                      attrs? (assoc :attrs x)
                      children? (assoc :children (into []
                                                       xf-ast children)))))
          (el? [el]
            (and (coll? el)
                 (keyword? (first el))))]
    (map (fn [el]
           (cond
             (el? el) (xf-ast-tag el)
             (coll? el) {:type     :fragment
                         :children (into [] xf-ast el)}
             :else {:type  :literal
                    :value el})))))

(defn ->ast
  [html]
  (first (sequence xf-ast [html])))

(defn ->html
  [{:keys [tag children value attrs]}]
  (cond
    (and tag attrs children) (into [tag attrs]
                                   (map ->html)
                                   children)
    (and tag children) (into [tag]
                             (map ->html)
                             children)
    (and tag attrs) [tag attrs]
    tag [tag]
    children (map ->html children)
    :else value))

(s/def ::node (s/keys :opt-un [::tag
                               ::children
                               ::value
                               ::attrs]))

(s/def ::tag keyword?)
(s/def ::children (s/coll-of ::node :kind vector?))
(s/def ::value any?)
(s/def ::attrs map?)

(s/fdef ->html
        :args (s/cat :node ::node))

(defn ->eql
  [{:keys [tag children attrs]}]
  {:type     :root
   :children (if (qualified-keyword? tag)
               (let [join? children
                     children (into []
                                    (comp (map ->eql)
                                          (mapcat :children))
                                    children)]
                 [(cond-> {:type         (if join?
                                           :join
                                           :prop)
                           :key          tag
                           :dispatch-key tag}
                          attrs (assoc :params attrs)
                          join? (assoc
                                  :query (eql/ast->query {:type     :root
                                                          :children children})
                                  :children children))])
               (into []
                     (comp (map ->eql)
                           (mapcat :children))
                     children))})


(s/fdef ->eql
        :args (s/cat :node ::node))


(defn xf-place
  [data]
  (map (fn [{:keys [tag children]
             :as   node}]
         (cond
           (contains? data tag) (if (= ">" (namespace tag))
                                  {:type     :fragment
                                   :children (into []
                                                   (xf-place (get data tag))
                                                   children)}
                                  (->ast (get data tag)))
           children (assoc node
                      :children (into []
                                      (xf-place data)
                                      children))
           :else node))))


(defn place
  [ast data]
  (first (sequence (xf-place data)
                   [ast])))


(s/fdef place
        :args (s/cat :node ::node
                     :data map?))


(defn env-placeholder-reader-v2
  [env]
  (if (p/placeholder-key? env (-> env :ast :dispatch-key))
    (let [params (-> env :ast :params)]
      (p/swap-entity! env merge params)
      (p/join env))
    ::p/continue))

(s/fdef env-placeholder-reader-v2
        :args (s/cat :env (s/keys)))

(def parser
  (p/parser {::p/plugins [(pc/connect-plugin)]
             ::p/env     {::p/reader               [p/map-reader
                                                    pc/reader2
                                                    pc/open-ident-reader
                                                    env-placeholder-reader-v2]
                          ::p/placeholder-prefixes #{">"}}}))

(s/fdef parser
        :args (s/cat :env (s/keys :req [::pc/indexes])
                     :tx ::eql/query)
        :ret map?)

(defn hparser-v2-impl
  [env ast]
  (let [query (eql/ast->query (->eql ast))]
    (if (empty? query)
      ast
      (let [result (reduce (fn [acc el]
                             (merge acc (parser env [el])))
                           {}
                           query)
            ast (place ast result)]
        (hparser-v2-impl env ast)))))

(defn hparser-v2
  [env html]
  (->> html
       ->ast
       (hparser-v2-impl env)
       ->html))
