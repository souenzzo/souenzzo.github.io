(ns br.com.souenzzo.rgt
  (:require [clojure.string :as string])
  #?(:clj (:import (clojure.lang Keyword))))

(defn render-properties
  [attr]
  (let [kvs (for [[k v] (dissoc attr :key :on-click)
                  :when (some? v)]
              [(name k) (pr-str v)])]
    (when (seq kvs)
      (string/join " " (cons ""
                         (for [[k v] kvs]
                           (str k "=" v)))))))

(defn render-to-static-markup
  [v]
  (cond
    (coll? v) (let [[tag & [attr & others :as raw-body]] v
                    attr? (map? attr)
                    body (if attr?
                           others
                           raw-body)]
                (cond
                  (keyword? tag) (let [tag (name tag)]
                                   (string/join
                                     (concat
                                       ["<" tag
                                        (render-properties (when attr?
                                                             attr))
                                        ">"]
                                       (map render-to-static-markup body)
                                       ["</" tag ">"])))
                  (fn? tag) (render-to-static-markup (apply tag raw-body))
                  :else (string/join (map render-to-static-markup v))))
    :else (str v)))
