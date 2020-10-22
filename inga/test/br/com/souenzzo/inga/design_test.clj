(ns br.com.souenzzo.inga.design-test
  (:require [clojure.test :refer [deftest]]
            [midje.sweet :refer [fact =>]]
            [com.wsscode.pathom.connect :as pc]
            [br.com.souenzzo.inga.connect :as ic]))

(pc/defresolver custom-world [env input]
  {:custom/world [:p "world"]})

(pc/defresolver custom-hello [env input]
  {:custom/hello [:div
                  [:p "hello"]
                  [:custom/world {}]]})

(def indexes (pc/register {} [custom-hello
                              custom-world]))

(deftest simple
  (fact
    (ic/hparser-v2 {::pc/indexes indexes}
                   [:div
                    [:custom/hello {}]])
    => [:div
        [:div
         [:p "hello"]
         [:p "world"]]]))
