(ns br.com.souenzzo.dvm
  (:require [clojure.string :as string]))

(defprotocol IEl
  :extend-via-metadata true
  (-el [this opts]))

(defprotocol IRender
  :extend-via-metadata true
  (-render [this ctx]))

(extend-protocol IRender
  String
  (-render [this _]
    this)
  clojure.lang.Fn
  (-render [this ctx]
    (this ctx)))


(extend-protocol IEl
  String
  (-el [this {::keys [children props]}]
    (fn [ctx]
      (str
        "<" this (when props
                   (str " " (string/join " " (for [[k v] props]
                                               (str (name k) "=" (str v))))))
        ">"
        (string/join (for [child children]
                       (-render child ctx)))
        "</" this ">"))))

(defn el
  ([op] (-el op {::op op}))
  ([op props] (-el op {::op    op
                       ::props props}))
  ([op props & children]
   (-el op {::op       op
            ::props    props
            ::children children})))