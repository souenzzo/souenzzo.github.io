(ns br.com.souenzzo.eql-refdb
  (:require [edn-query-language.core :as eql]))

(defn- ref?
  [x]
  (and (coll? x)
       (keyword? (first x))))

(defn db->tree
  [{::keys [db entity query]}]
  (if entity
    (let [{:keys [children]} (eql/query->ast query)]
      (into {}
            (for [{:keys [dispatch-key children]} children
                  :let [value (get entity dispatch-key)]]
              (if children
                [dispatch-key (if (ref? value)
                                (db->tree {::db     db
                                           ::entity (get-in db value)
                                           ::query  (eql/ast->query {:type :root :children children})})
                                (vec (for [ref value]
                                       (db->tree {::db     db
                                                  ::entity (get-in db ref)
                                                  ::query  (eql/ast->query {:type :root :children children})}))))]
                [dispatch-key value]))))
    (let [{:keys [children]} (-> (eql/query->ast query))]
      (if children
        (db->tree
          {::query  (eql/ast->query {:type :root :children children})
           ::db     db
           ::entity db})))))

(defn merge-tree-node
  [db tree {:keys [dispatch-key children meta]
            :as   node}]
  (cond
    (contains? tree dispatch-key) (cond
                                    (symbol? dispatch-key) (let [tree (get tree dispatch-key)]
                                                             (reduce
                                                               (fn [db node]
                                                                 (merge-tree-node db tree
                                                                                  node))
                                                               db
                                                               children))
                                    children (if (contains? meta :index)
                                               (let [index (:index meta)
                                                     tree' (get tree dispatch-key)
                                                     many? (and (coll? tree')
                                                                (not (map? tree')))]
                                                 (if many?
                                                   (let [els (map (fn [tree]
                                                                    {:tree tree
                                                                     :ref  (with-meta (vec (find tree index))
                                                                                      {:ref true})})
                                                                  tree')]
                                                     (reduce (fn [db {:keys [ref tree]}]
                                                               (let [db' (get-in db ref)
                                                                     db-after (reduce
                                                                                (fn [db node]
                                                                                  (merge-tree-node db tree
                                                                                                   node))
                                                                                db'
                                                                                children)]
                                                                 (-> db
                                                                     (update dispatch-key conj ref)
                                                                     (assoc-in ref db-after))))
                                                             (assoc db dispatch-key [])
                                                             els))
                                                   (let [ref (with-meta (vec (find tree' index))
                                                                        {:ref true})
                                                         db' (get-in db ref)
                                                         db-after (reduce
                                                                    (fn [db node]
                                                                      (merge-tree-node db tree'
                                                                                       node))
                                                                    db'
                                                                    children)]
                                                     (-> db
                                                         (assoc dispatch-key ref)
                                                         (assoc-in ref db-after)))))
                                               (let [db' (get db dispatch-key)
                                                     tree' (get tree dispatch-key)
                                                     db-after (reduce
                                                                (fn [db node]
                                                                  (merge-tree-node db tree'
                                                                                   node))
                                                                db'
                                                                children)]
                                                 (assoc db dispatch-key db-after)))
                                    :else (assoc db dispatch-key (get tree dispatch-key)))
    children (reduce
               (fn [db node]
                 (merge-tree-node db tree node))
               db
               children)
    :else db))

(defn tree->db
  [{::keys [db tree tx]}]
  (merge-tree-node db tree (eql/query->ast tx)))
