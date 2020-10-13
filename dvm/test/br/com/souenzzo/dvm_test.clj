(ns br.com.souenzzo.dvm-test
  (:require [clojure.test :refer [deftest testing is]]
            [br.com.souenzzo.dvm :as dvm :refer [el]]
            [clojure.pprint :as pp]))


(deftest nested
  (is (= "<div foo=42>hey<div foo=42>hey</div></div>"
         (dvm/-render (el :div
                          {:foo 42}
                          "hey"
                          (el :div
                              {:foo 42}
                              "hey"))

                      {}))))

(deftest with-function
  (is (= "<div foo=42>hey<div>with-fn-42</div></div>"
         (dvm/-render (el :div
                          {:foo 42}
                          "hey"
                          (el (fn [ctx {:keys [foo]}]
                                (el :div
                                    {}
                                    (str "with-fn-" foo)))
                              {:foo 42}))

                      {}))))
