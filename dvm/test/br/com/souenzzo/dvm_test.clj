(ns br.com.souenzzo.dvm-test
  (:require [clojure.test :refer [deftest testing is]]
            [br.com.souenzzo.dvm :as dvm]
            [midje.sweet :refer [fact =>]]))

(defn ui-sum
  [{:keys [a]} {:keys [b]}]
  (if b
    [:ul
     [:li [:p (str (+ a b))]]
     [:li [ui-sum {}]]]
    [:p a]))

(deftest hello-world
  (fact
    (dvm/render-to-string {} [:div "Hello World!"])
    => "<div>Hello World!</div>")
  (fact
    (dvm/render-to-string {} [:div [:div "Hello"]])
    => "<div><div>Hello</div></div>")
  (fact
    (dvm/render-to-string {} [:div {:id "42" :value 42} "Hello"])
    => "<div id=\"42\" value=42>Hello</div>")
  (fact
    (dvm/render-to-string {:a 1}
                          [:div [ui-sum {:b 2}]])
    => "<div><ul><li><p>3</p></li><li><p>1</p></li></ul></div>")
  (fact
    (dvm/render-to-string {}
                          [:div
                           [:p {} "oi"]
                           (for [i (range 2)]
                             [:p (str "oi" i)])])
    => "<div><p>oi</p><p>oi0</p><p>oi1</p></div>")
  (fact
    (dvm/render-to-string {}
                          [:meta {:charset "utf-8"}])
    => "<meta charset=\"utf-8\">"))
