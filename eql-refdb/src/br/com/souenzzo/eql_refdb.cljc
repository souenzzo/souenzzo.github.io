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
    (let [{:keys [key children]} (-> (eql/query->ast query)
                                     :children
                                     first)]
      (if children
        (db->tree
          {::query  (eql/ast->query {:type :root :children children})
           ::db     db
           ::entity (get-in db key)})))))

(defn tree->db-node
  [{::keys [db node value]}]
  (let [{:keys [dispatch-key children]} node]
    (cond
      (nil? dispatch-key) db
      :else db)))

(defn tree->db
  [{::keys [db tx result]}]
  (let [node (eql/query->ast tx)]
    (tree->db-node {::db    db
                    ::node  node
                    ::value result})))

(defn tree->db'
  [{::keys [db value query attribute->index]}]
  (reduce
    (fn [db {:keys [key children query]}]
      (cond
        (ref? key) (if children
                     (let [current-value (get value key)
                           final-value (into {}
                                             (map (fn [{:keys [dispatch-key children] :as node}]
                                                    (let [final-value (get current-value dispatch-key)
                                                          index-key (or (get attribute->index dispatch-key)
                                                                        (-> node :meta :ident)
                                                                        (-> final-value meta :ident))]
                                                      [dispatch-key (if children
                                                                      (if (sequential? final-value)
                                                                        (mapv #(find % index-key)
                                                                              final-value)
                                                                        (find final-value index-key))
                                                                      final-value)])))
                                             children)
                           db (reduce
                                (fn [db {:keys [dispatch-key children]
                                         :as   node}]
                                  (let [final-value (get current-value dispatch-key)
                                        index-key (or (get attribute->index dispatch-key)
                                                      (-> node :meta :ident)
                                                      (-> final-value meta :ident))]
                                    (if children
                                      (if (sequential? final-value)
                                        (reduce (fn [db final-value]
                                                  (when-not index-key
                                                    (throw (ex-info "aa" {})))
                                                  (tree->db {::db               db
                                                             ::value            {(find final-value index-key) final-value}
                                                             ::query            (eql/ast->query {:type     :root
                                                                                                 :children [{:type         :join
                                                                                                             :key          (find final-value index-key)
                                                                                                             :dispatch-key index-key
                                                                                                             :children     children}]})
                                                             ::attribute->index attribute->index}))
                                                db final-value)
                                        (tree->db {::db               db
                                                   ::value            {(find final-value index-key) final-value}
                                                   ::query            (eql/ast->query {:type     :root
                                                                                       :children [{:type         :join
                                                                                                   :key          (find final-value index-key)
                                                                                                   :dispatch-key index-key
                                                                                                   :children     children}]})
                                                   ::attribute->index attribute->index}))
                                      db)))

                                db
                                children)]
                       (update-in db key merge final-value))
                     #_todo db)
        (contains? value key) (tree->db {::db               db
                                         ::value            (get value key)
                                         ::query            query
                                         ::attribute->index attribute->index})
        :else db))

    db
    (-> query eql/query->ast :children)))




(defn merge-tree
  [db tree {:keys [dispatch-key children meta]
            :as   node}]
  (cond
    (contains? tree dispatch-key) (cond
                                    (symbol? dispatch-key) (let [tree (get tree dispatch-key)]
                                                             (reduce
                                                               (fn [db node]
                                                                 (merge-tree db tree
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
                                                                                  (merge-tree db tree
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
                                                                      (merge-tree db tree'
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
                                                                  (merge-tree db tree'
                                                                              node))
                                                                db'
                                                                children)]
                                                 (assoc db dispatch-key db-after)))
                                    :else (assoc db dispatch-key (get tree dispatch-key)))
    children (reduce
               (fn [db node]
                 (merge-tree db tree node))
               db
               children)
    :else db))


