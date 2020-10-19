(ns br.com.souenzzo.inga.connect-test
  (:require [clojure.test :refer [deftest]]
            [br.com.souenzzo.inga.connect :as ic]
            [midje.sweet :refer [fact =>]]))

(deftest simple
  (fact
    (ic/query [:div
               [:ui/table]])
    => [:ui/table])
  (fact
    (ic/query [:div
               [:p
                [:ui/table]]])
    => [:ui/table])
  (fact
    (ic/query [:div
               [:p
                [:ui/table
                 [:ui/table]]
                [:ui/list]]])
    => [{:ui/table [:ui/table]}
        :ui/list]))

(deftest placeholder
  (fact
    (ic/query [:div
               [:>/with-data {:a 42}
                [:ui/table]]])
    => `[{(:>/with-data {:a 42})
          [:ui/table]}]))
