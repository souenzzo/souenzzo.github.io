(ns br.com.souenzzo.rgt-test
  (:require [br.com.souenzzo.rgt :as rgt]
            #?(:cljs [reagent.dom.server :as rd])
            [clojure.test :refer [deftest is testing]]))

(defn render [x]
  #?(:cljs    (rd/render-to-static-markup x)
     :default (rgt/render-to-static-markup x)))
(deftest hello
  (let [hello-world [:div "Hello World"]]
    (is (= "<div>Hello World</div>"
          (render hello-world))))
  (let [hello-p-world [:div "Hello"
                       [:p "World"]]]
    (is (= "<div>Hello<p>World</p></div>"
          (render hello-p-world)))))
(defn ui-todo-list
  []
  [:div
   "Todo List"
   (for [i (range 3)]
     [:div {:key   i
            :class (when (odd? i)
                     "active")}
      (str "todo-" i)])])

(deftest ui-todo-list-test
  (let [hello-world [ui-todo-list]]
    (is (= "<div>Todo List<div>todo-0</div><div class=\"active\">todo-1</div><div>todo-2</div></div>"
          (render hello-world)))))


(deftest on-click
  (is (= "<button class=\"wow\">ok</button>"
        (render [:button {:class    "wow"
                          :on-click (fn []
                                      "hello")}
                 "ok"]))))


(comment
  (-> `shadow.cljs.devtools.server/start!
    requiring-resolve
    (apply []))
  (-> `shadow.cljs.devtools.api/watch
    requiring-resolve
    (apply [:node-tests])))