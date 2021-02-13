(ns br.com.souenzzo.mpr.main-test
  (:require [datomic.api :as d]
            [clojure.test :refer [deftest]]
            [midje.sweet :refer [fact =>]]
            [clojure.core.async :as async])
  (:import (java.util.concurrent TimeUnit BlockingQueue)
           (java.time Duration)))

(set! *warn-on-reflection* true)

(defn connection-released?
  [ex]
  (let [{:keys [cognitect.anomalies/category
                db/error]} (ex-data ex)]
    (and (= category :cognitect.anomalies/conflict)
         (= error :db.error/connection-released))))

(defn tx-report
  "conn - A datomic connection
   duration - The duration of pooling.
   c - a core.async channel"
  [conn ^Duration duration c]
  (let [queue ^BlockingQueue (d/tx-report-queue conn)]
    (async/thread
      (try
        (loop []
          (let [vs (.poll queue (.toNanos duration)
                          TimeUnit/NANOSECONDS)]
            (when (every? #(async/put! % c) vs)
              (d/db conn)
              (recur))))
        (catch IllegalStateException ex
          (when-not (connection-released? ex)
            (throw ex)))))
    c))


(deftest watch-conn
  (let [conn (-> "datomic:mem://mpr"
                 (doto d/delete-database
                       d/create-database)
                 (d/connect))
        txs (tx-report conn
                       (Duration/ofSeconds 1)
                       (async/chan))]
    (async/go
      (loop []
        (when-let [x (async/<! txs)]
          (prn x)))
      (prn [:exit txs]))
    (fact
      (+ 1 2)
      => {})))
