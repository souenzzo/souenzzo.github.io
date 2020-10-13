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


(defn tree->db
  [{::keys []}])

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


