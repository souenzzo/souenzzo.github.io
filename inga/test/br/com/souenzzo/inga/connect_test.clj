(ns br.com.souenzzo.inga.connect-test
  (:require [clojure.test :refer [deftest]]
            [br.com.souenzzo.inga.connect :as ic]
            [midje.sweet :refer [fact =>]]
            [clojure.spec.test.alpha :as stest]
            [edn-query-language.core :as eql]))

(stest/instrument)

(deftest ast
  (fact
    "just div"
    (ic/->ast [:div])
    => {:type :element
        :tag  :div})
  (fact
    "empty attrs"
    (ic/->ast [:div {}])
    => {:type  :element
        :attrs {}
        :tag   :div})
  (fact
    "actual attrs"
    (ic/->ast [:div {:a 42}])
    => {:type  :element
        :attrs {:a 42}
        :tag   :div})
  (fact
    "children"
    (ic/->ast [:div [:div]])
    => {:type     :element
        :tag      :div
        :children [{:type :element
                    :tag  :div}]})
  (fact
    "frag"
    (ic/->ast [:div
               (for [i (range 2)]
                 [:div])])
    => {:tag      :div
        :type     :element
        :children [{:children [{:tag :div :type :element}
                               {:tag :div :type :element}]
                    :type     :fragment}]})
  (fact
    "literal"
    (ic/->ast [:div
               "a"])
    => {:tag      :div
        :type     :element
        :children [{:type  :literal
                    :value "a"}]}))


(defn ->eql
  [html]
  (eql/ast->query (ic/->eql (ic/->ast html))))

(deftest html->eql
  (fact
    "simple"
    (->eql [:div
            [:ui/table]])
    => [:ui/table])
  (fact
    (->eql [:div
            [:p
             [:ui/table]]])
    => [:ui/table])
  (fact
    "nested"
    (->eql [:div
            [:p
             [:ui/table
              [:ui/table]]
             [:ui/list]]])
    => [{:ui/table [:ui/table]}
        :ui/list])
  (fact
    "placeholder"
    (->eql [:div
            [:>/with-data {:a 42}
             [:ui/table]]])
    => `[({:>/with-data
           [:ui/table]}
          {:a 42})]))

(defn place
  [html data]
  (ic/->html (ic/place (ic/->ast html)
                       data)))

(deftest apply-data
  (fact
    (place
      [:div [:ui/table]]
      {:ui/table [:table]})
    => [:div [:table]])
  (fact
    (place
      [:div
       [:>/env {:a 42}
        [:ui/table]]]
      {:>/env {:ui/table [:table]}})
    => `[:div ([:table])]))
