(ns ipfs.main-test
  (:require [clojure.test :refer [deftest]]
            [ipfs.main :as ipfs]
            [midje.sweet :refer [fact =>]])
  (:import (java.io OutputStream)))

(set! *warn-on-reflection* true)

(deftest hello
  (let []
    (fact
      (ipfs/write "hello")
      => {})))



