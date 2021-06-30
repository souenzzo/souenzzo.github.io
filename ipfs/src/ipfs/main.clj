(ns ipfs.main
  (:refer-clojure :exclude [read])
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.core.async :as async])
  (:import (java.io PushbackReader Writer BufferedReader)
           (clojure.lang IDeref)
           (java.util.function Consumer)))

(set! *warn-on-reflection* true)
*data-readers*
(defn ^Process start
  [{::keys [command
            arguments
            environment
            on-output]}]
  (let [pb (new ProcessBuilder ^"[Ljava.lang.String;"
             (into-array String (into [command]
                                  arguments)))
        env (.environment pb)
        _ (do (.clear env)
              (doseq [[k v] environment]
                (.put env k v)))
        p (.start pb)]
    (when on-output
      (let [stdout (.lines (BufferedReader. (io/reader (.getInputStream p))))
            stderr (.lines (BufferedReader. (io/reader (.getErrorStream p))))]
        (async/thread
          (.forEach stderr (reify Consumer
                             (accept [this v]
                               (on-output :stderr v)))))

        (async/thread
          (.forEach stdout (reify Consumer
                             (accept [this v]
                               (on-output :stdout v)))))))
    p))



(def daemon
  ;; sudo sysctl -w net.core.rmem_max=2500000
  (delay
    (.waitFor (-> {::command     "ipfs"
                   ::arguments   ["init"]
                   ::environment {"IPFS_PATH" "ipfs"}}
                start))
    (let [ipfs (-> {::command     "ipfs"
                    ::arguments   ["daemon"]
                    ::on-output   (fn [level v]
                                    (case level
                                      :stderr (log/error v)
                                      (log/info v)))
                    ::environment {"IPFS_PATH" "ipfs"}}
                 start)]
      ipfs)))


(defn read
  [addr]
  @daemon
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

(defmethod print-method Edn [edn ^Writer w]
  (.write w (str "#ipfs/cid \"" (:cid edn) "\"")))

(defmethod print-dup Edn [o w]
  (print-method o w))

(defn ->ref [cid] (->Edn cid))

(defn write
  [v]
  @daemon
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
