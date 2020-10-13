(ns br.com.souenzzo.dvm
  (:require [clojure.string :as string])
  (:import (clojure.lang Fn Keyword)))

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
  Fn
  (-render [this ctx]
    (-render (this ctx)
             ctx)))


(extend-protocol IEl
  Keyword
  (-el [this opts]
    (-el (name this) opts))
  String
  (-el [this {::keys [children props]}]
    (fn [ctx]
      (str
        "<" this (when-not (empty? props)
                   (str " " (string/join " " (for [[k v] props]
                                               (if (boolean? v)
                                                 (name k)
                                                 (str (name k) "=" (str v)))))))
        ">"
        (string/join (for [child children]
                       (-render child ctx)))
        "</" this ">")))
  Fn
  (-el [this {::keys [children props]}]
    (fn [ctx]
      (apply this ctx props children))))

(defn el
  ([op] (-el op {::op op}))
  ([op props-or-el] (-el op (if (map? props-or-el)
                              {::op    op
                               ::props props-or-el}
                              {::op       op
                               ::children [props-or-el]})))
  ([op props & children]
   (-el op {::op       op
            ::props    props
            ::children children})))