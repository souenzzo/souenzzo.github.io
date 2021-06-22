(ns ipfs.main
  (:refer-clojure :exclude [read])
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:import (java.io PushbackReader)
           (clojure.lang IDeref IMeta IRecord)))

(set! *warn-on-reflection* true)

(defn read
  [addr]
  (let [bp (new ProcessBuilder ^"[Ljava.lang.String;"
             (into-array String ["ipfs" "cat" addr]))
        p (.start bp)
        result (with-open [stdout (.getInputStream p)
                           rdr (io/reader stdout)]
                 (edn/read (PushbackReader. rdr)))]
    (.waitFor p)
    result))

(defrecord Edn [cid]
  IDeref
  (deref [this]
    (read cid)))

(defmethod print-method Edn [edn ^java.io.Writer w]
  (.write w (str "#ipfs \"" (:cid edn) "\"")))

(defmethod print-dup Edn [o w]
  (print-method o w))

(defn ->ref [cid] (->Edn cid))

(defn write
  [v]
  (let [bp (new ProcessBuilder ^"[Ljava.lang.String;"
             (into-array String ["ipfs" "add" "-"]))
        p (.start bp)
        addr (with-open [stdout (.getInputStream p)]
               (with-open [stdin (.getOutputStream p)
                           out (io/writer stdin)]
                 (binding [*out* out]
                   (pr v)))
               (second (string/split (slurp stdout) #"\s" 3)))]
    (.waitFor p)
    (->ref addr)))
