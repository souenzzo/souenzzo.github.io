(ns br.com.souenzzo.eql-refdb-test
  (:require [br.com.souenzzo.eql-refdb :as refdb]
            [clojure.test :refer [deftest is testing]]
            [clojure.pprint :as pp]
            [edn-query-language.core :as eql]))

(deftest db->tree
  (is (= {:user/contatcs [{:user/name "refdb"}]
          :user/id       1
          :user/name     "refdb"}
         (refdb/db->tree
           {::refdb/db    {:user/id {1 {:user/id       1
                                        :user/name     "refdb"
                                        :user/contatcs [[:user/id 1]]}}}
            ::refdb/query [{[:user/id 1] [:user/id
                                          :user/name
                                          {:user/contatcs [:user/name]}]}]})))
  (is (= {:user/contatcs {:user/name "refdb"}
          :user/id       1
          :user/name     "refdb"}
         (refdb/db->tree
           {::refdb/db    {:user/id {1 {:user/id       1
                                        :user/name     "refdb"
                                        :user/contatcs [:user/id 1]}}}
            ::refdb/query [{[:user/id 1] [:user/id
                                          :user/name
                                          {:user/contatcs [:user/name]}]}]})))
  (is (= {:user/contatcs {:user/contatcs {:user/contatcs {:user/contatcs {:user/name "refdb"}}}
                          :user/name     "refdb"}
          :user/id       1
          :user/name     "refdb"}
         (refdb/db->tree
           {::refdb/db    {:user/id {1 {:user/id       1
                                        :user/name     "refdb"
                                        :user/contatcs [:user/id 1]}}}
            ::refdb/query [{[:user/id 1] [:user/id
                                          :user/name
                                          {:user/contatcs [:user/name
                                                           {:user/contatcs [{:user/contatcs [{:user/contatcs [:user/name]}]}]}]}]}]}))))


(deftest ignore-mutations
  (is (= {:user/id {1 {:user/id   1
                       :user/name "refdb"}}}
         (refdb/tree->db
           {::refdb/attribute->index {}
            ::refdb/value            {'foo {[:user/id 1] {:user/id   1
                                                          :user/name "refdb"}}}
            ::refdb/query            '[{(foo {:bar 42})
                                        [{[:user/id 1] [:user/id
                                                        :user/name]}]}]}))))

(deftest tree->db
  (is (= {:user/id {1 {:user/id   1
                       :user/name "refdb"}}}
         (refdb/tree->db
           {::refdb/attribute->index {}
            ::refdb/value            {[:user/id 1] {:user/id   1
                                                    :user/name "refdb"}}
            ::refdb/query            [{[:user/id 1] [:user/id
                                                     :user/name]}]})))
  (is (= {:user/id {1 {:user/id       1
                       :user/name     "refdb"
                       :user/contatcs [[:user/id 1]]}}}
         (refdb/tree->db
           {::refdb/attribute->index {:user/contatcs :user/id}
            ::refdb/value            {[:user/id 1] {:user/id       1
                                                    :user/name     "refdb"
                                                    :user/contatcs [{:user/id 1}]}}
            ::refdb/query            [{[:user/id 1] [:user/id
                                                     :user/name
                                                     {:user/contatcs [:user/id]}]}]}))))

(deftest tree->db-ref-to-one
  (testing
    "attribute->index at env"
    (is (= {:user/id {1 {:user/id      1
                         :user/name    "refdb"
                         :user/contatc [:user/id 2]}
                      2 {:user/id   2
                         :user/name "refrefdb"}}}
           (-> {::refdb/attribute->index {:user/contatc :user/id}
                ::refdb/value            {[:user/id 1] {:user/id      1
                                                        :user/name    "refdb"
                                                        :user/contatc {:user/id   2
                                                                       :user/name "refrefdb"}}}
                ::refdb/query            [{[:user/id 1] [:user/id
                                                         :user/name
                                                         {:user/contatc [:user/id
                                                                         :user/name]}]}]}
               #_(doto prn)
               refdb/tree->db))))
  (testing
    "attribute->index at query"
    (is (= {:user/id {1 {:user/id      1
                         :user/name    "refdb"
                         :user/contatc [:user/id 2]}
                      2 {:user/id   2
                         :user/name "refrefdb"}}}
           (-> {::refdb/value {[:user/id 1] {:user/id      1
                                             :user/name    "refdb"
                                             :user/contatc {:user/id   2
                                                            :user/name "refrefdb"}}}
                ::refdb/query [{[:user/id 1] [:user/id
                                              :user/name
                                              ^{:ident :user/id} {:user/contatc [:user/id
                                                                                 :user/name]}]}]}
               #_(doto prn)
               refdb/tree->db))))
  (testing
    "attribute->index at value"
    (is (= {:user/id {1 {:user/id      1
                         :user/name    "refdb"
                         :user/contatc [:user/id 2]}
                      2 {:user/id   2
                         :user/name "refrefdb"}}}
           (-> {::refdb/value {[:user/id 1] {:user/id      1
                                             :user/name    "refdb"
                                             :user/contatc ^{:ident :user/id}
                                                           {:user/id   2
                                                            :user/name "refrefdb"}}}
                ::refdb/query [{[:user/id 1] [:user/id
                                              :user/name
                                              {:user/contatc [:user/id
                                                              :user/name]}]}]}
               #_(doto prn)
               refdb/tree->db)))))


(deftest tree->db-ref-to-many
  (testing
    "attribute->index at env"
    (is (= {:user/id {1 {:user/id       1
                         :user/name     "refdb"
                         :user/contatcs [[:user/id 1]
                                         [:user/id 2]]}
                      2 {:user/id   2
                         :user/name "refrefdb"}}}
           (-> {::refdb/attribute->index {:user/contatcs :user/id}
                ::refdb/value            {[:user/id 1] {:user/id       1
                                                        :user/name     "refdb"
                                                        :user/contatcs [{:user/id   1
                                                                         :user/name "refdb"}
                                                                        {:user/id   2
                                                                         :user/name "refrefdb"}]}}
                ::refdb/query            [{[:user/id 1] [:user/id
                                                         :user/name
                                                         {:user/contatcs [:user/id
                                                                          :user/name]}]}]}
               #_(doto prn)
               refdb/tree->db))))
  (testing
    "attribute->index at query"
    (is (= {:user/id {1 {:user/id       1
                         :user/name     "refdb"
                         :user/contatcs [[:user/id 1]
                                         [:user/id 2]]}
                      2 {:user/id   2
                         :user/name "refrefdb"}}}
           (-> {::refdb/value {[:user/id 1] {:user/id       1
                                             :user/name     "refdb"
                                             :user/contatcs [{:user/id   1
                                                              :user/name "refdb"}
                                                             {:user/id   2
                                                              :user/name "refrefdb"}]}}
                ::refdb/query [{[:user/id 1] [:user/id
                                              :user/name
                                              ^{:ident :user/id} {:user/contatcs [:user/id
                                                                                  :user/name]}]}]}
               #_(doto prn)
               refdb/tree->db))))
  (testing
    "attribute->index at value"
    (is (= {:user/id {1 {:user/id       1
                         :user/name     "refdb"
                         :user/contatcs [[:user/id 1]
                                         [:user/id 2]]}
                      2 {:user/id   2
                         :user/name "refrefdb"}}}
           (-> {::refdb/value {[:user/id 1] {:user/id       1
                                             :user/name     "refdb"
                                             :user/contatcs ^{:ident :user/id}
                                                            [{:user/id   1
                                                              :user/name "refdb"}
                                                             {:user/id   2
                                                              :user/name "refrefdb"}]}}
                ::refdb/query [{[:user/id 1] [:user/id
                                              :user/name
                                              {:user/contatcs [:user/id
                                                               :user/name]}]}]}
               #_(doto prn)
               refdb/tree->db)))))

(deftest tree->db-v2
  (is (= {:a 42}
         (refdb/tree->db
           {::tx    [:a]
            ::value {:a 42}}))))


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
                                                                     (update dispatch-key (fnil conj []) ref)
                                                                     (assoc-in ref db-after))))
                                                             db
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

(deftest merge-tree-test
  (is (= {:a 33}
         (merge-tree {:a 42}
                     {:a 33}
                     (eql/query->ast [:a]))))
  (is (= {:a 42}
         (merge-tree {:a 42}
                     {:a 33}
                     (eql/query->ast [:b]))))
  (is (= {:a 33}
         (merge-tree {:a 42}
                     `{inc {:a 33}}
                     (eql/query->ast `[{(inc {}) [:a]}]))))
  (is (= {:a {:b 33}}
         (merge-tree {:a {:b 42}}
                     `{:a {:b 33}}
                     (eql/query->ast `[{:a [:b]}]))))
  (is (= {:a {:b 33}}
         (merge-tree {}
                     `{:a {:b 33}}
                     (eql/query->ast `[{:a [:b]}]))))
  (is (= {:a {:b 33
              :c 22}}
         (merge-tree {:a {:b 42
                          :c 22}}
                     `{:a {:b 33}}
                     (eql/query->ast `[{:a [:b]}]))))
  (is (= {:a {:b 33}}
         (merge-tree {:a {:b 42
                          :c 22}}
                     `{:a {:b 33}}
                     (eql/query->ast `[:a]))))
  (is (= {:a ^:ref [:b 33]
          :b {33 {:b 33
                  :c 22}}}
         (merge-tree {}
                     `{:a {:b 33
                           :c 22}}
                     (eql/query->ast [^{:index :b} {:a [:b
                                                        :c]}]))))
  (is (= {:a [^:ref [:b 33]]
          :b {33 {:b 33
                  :c 22}}}
         (merge-tree {}
                     `{:a [{:b 33
                            :c 22}]}
                     (eql/query->ast [^{:index :b} {:a [:b
                                                        :c]}])))))

