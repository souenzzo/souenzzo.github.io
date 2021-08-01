(ns br.com.souenzzo.wtf-test
  (:require [clojure.test :refer [deftest]]
            [br.com.souenzzo.wtf :as wtf]
            [midje.sweet :refer [fact =>]]))


(deftest hello
  (let [add (wtf/func
              '(param $a i64)
              '(param $b i64)
              '(result i64)
              #_'(func $getAnswer (result i32)
                   i32.const 42)
              'local.get '$a
              'local.get '$b
              'i64.add)]
    (fact
      (.asLong (add 1 2))
      => 3)))
