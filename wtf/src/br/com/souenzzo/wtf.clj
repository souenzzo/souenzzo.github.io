(ns br.com.souenzzo.wtf
  (:require [clojure.java.shell :as sh]
            [clojure.java.io :as io])
  (:import (java.io File ByteArrayOutputStream)
           (org.graalvm.polyglot Context Source Value)
           (org.graalvm.polyglot.io ByteSequence)))

(set! *warn-on-reflection* true)

(defn wat->wasm
  [s]
  (let [stdin (File/createTempFile "wat" "in")
        _ (spit stdin (str s))
        stdout (File/createTempFile "wat" "out")
        {:keys [out err exit]} (sh/sh "wat2wasm" (.getAbsolutePath stdin)
                                 "-o" (.getAbsolutePath stdout))
        _ (println out)
        _ (println err)
        baos (ByteArrayOutputStream.)]
    (io/copy stdout baos)
    (.delete stdin)
    (.delete stdout)
    (.toByteArray baos)))

(defn ^Value wtf!
  [s]
  (println s)
  (let [ctx (.build (Context/newBuilder (into-array ["wasm"])))
        src (.build (Source/newBuilder
                      "wasm"
                      (ByteSequence/create (wat->wasm s))
                      "hello.wat"))]
    (.eval ctx src)
    (.getMember (.getBindings ctx "wasm") "main")))


(defn func
  [& ops]
  (let [f (.getMember (wtf! `(~'module
                               (~'func (~'export "inline")
                                 ~@ops)))
            "inline")]
    (fn [& vs]
      (.execute f (into-array Object vs)))))




