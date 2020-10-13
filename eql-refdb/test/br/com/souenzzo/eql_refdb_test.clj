(ns br.com.souenzzo.eql-refdb-test
  (:require [br.com.souenzzo.eql-refdb :as refdb]
            [clojure.test :refer [deftest is testing]]))

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
