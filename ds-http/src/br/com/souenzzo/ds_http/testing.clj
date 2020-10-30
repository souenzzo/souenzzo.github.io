(ns br.com.souenzzo.ds-http.testing
  (:require [clojure.string :as string]
            [br.com.souenzzo.ds-http :as ds])
  (:import (java.io ByteArrayOutputStream ByteArrayInputStream)))

(set! *warn-on-reflection* true)

(extend-protocol ds/IOutputStream

  ByteArrayOutputStream
  (-write [this b]
    (.write this (int b))
    this))

(extend-protocol ds/IInputStream
  ByteArrayInputStream
  (-read [this]
    (.read this)))

(defn str->is
  [s]
  (ByteArrayInputStream. (.getBytes (str s))))


(defn request->input-stream
  [{:ring.request/keys [path query headers method body protocol]
    :or                {protocol "HTTP/1.1"}}]
  (str->is
    (str (string/upper-case (name method))
         " "
         path query
         " " protocol
         (reduce-kv (fn [acc k v]
                      (str acc
                           k ":" v "\r\n"))
                    "\r\n"
                    headers)
         "\r\n"
         (str body))))
