(ns br.com.souenzzo.dvm-test
  (:require [clojure.test :refer [deftest testing is]]
            [br.com.souenzzo.dvm :as dvm :refer [el]]))


(deftest foo
  (let []
    (testing
      (is (= "<div foo=42>hey<div foo=42>hey</div></div>"
             ((el "div"
                  {:foo 42}
                  "hey"
                  (el "div"
                      {:foo 42}
                      "hey"))

              {}))))))

