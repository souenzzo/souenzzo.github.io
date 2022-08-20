(ns foo.core
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clj-async-profiler.core :as prof]
    [clojure.spec.alpha :as s])
  (:import (java.io BufferedReader File)))
;; sudo sysctl -w kernel.perf_event_paranoid=1
;; sudo sysctl -w kernel.kptr_restrict=0
(set! *warn-on-reflection* true)
(defn gen-data
  [^File f size]
  (.mkdirs (.getParentFile f))
  (with-open [w (io/writer f)]
    (dotimes [_ size]
      (json/write (rand-int 1e4) w)
      (.append w \newline))))

(s/def ::acc number?)
(s/def ::n nat-int?)
(s/def ::avg
  (s/keys :req [::acc ::n]))
(defn avg-combine
  [a b]
  {::acc (+ (::acc a)
           (::acc b))
   ::n   (+ (::n a)
           (::n b))})

(defn ->avg
  ([] {::acc 0
       ::n   0})
  ([v] {::acc v
        ::n   1})
  ([v1 v2] {::acc (+ v1 v2)
            ::n   2})
  ([v1 v2 & vs]
   (reduce avg-combine
     (->avg v1 v2)
     (map ->avg vs))))

(defn ->result
  [{::keys [acc n]}]
  (/ acc n))


(defn do-avg-json
  [f]
  (with-open [rdr (io/reader f)]
    (loop [avg (->avg)]
      (let [v (json/read rdr
                :eof-error? false
                :eof-value rdr)]
        (if (identical? v rdr)
          (->result avg)
          (recur (avg-combine avg (->avg 1))))))))


(defn do-avg-parse
  [f]
  (with-open [^BufferedReader rdr (io/reader f)]
    (loop [acc 0
           n 0]
      (if-let [v (some-> rdr .readLine Long/parseLong)]
        (recur (long (+ acc v))
          (inc n))
        (/ acc n)))))


;; serial vs lazy vs paralelo vs async


(defn foo
  []
  :ok)


(comment
  (prof/serve-files 8080)
  (def data-file (io/file "target" "data.jsonl"))
  (time (gen-data data-file 1e1))
  (double (time (prof/profile (do-avg-json data-file))))
  (double (time (prof/profile (do-avg-parse data-file)))))

