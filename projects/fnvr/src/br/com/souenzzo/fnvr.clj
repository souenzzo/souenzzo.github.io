(ns br.com.souenzzo.fnvr
  (:require [clojure.data.json :as json]
            [clojure.string :as string])
  (:import (java.util Date)
           (java.time Instant)
           (java.net URLEncoder)
           (java.nio.charset StandardCharsets)
           (java.net.http HttpClient)))

(set! *warn-on-reflection* true)
(defn query
  [sym]
  (-> (str "https://search.maven.org/solrsearch/select?q=" sym)
    slurp
    (json/read-str :key-fn keyword)
    :response
    :docs
    (->> (filter (fn [{:keys [id]}]
                   (= id
                     (str (namespace sym)
                       ":"
                       (name sym))))))
    first))
(def *client
  (delay (HttpClient/newHttpClient)))
(defn select
  [params]
  (let [{:keys [responseHeader
                response]} (-> (str "https://search.maven.org/solrsearch/select?"
                                 (string/join "&"
                                   (for [[k v] params]
                                     (str (URLEncoder/encode (name k) StandardCharsets/UTF_8)
                                       "="
                                       (URLEncoder/encode (str v) StandardCharsets/UTF_8)))))
                             ;; (doto prn)
                             slurp
                             (json/read-str :key-fn keyword))
        {:keys [docs start]} response]
    (when (seq docs)
      (lazy-cat docs
        (select (assoc params :start (+ start (count docs))))))))


(defn version-history
  [sym]
  (-> (select {:q    (str "g:"
                       (namespace sym)
                       " AND a:"
                       (name sym)
                       "")
               :core "gav"})
    (->> (map (fn [{:keys [timestamp v]}]
                {:mvn/version v
                 :sym         sym
                 :timestamp   (Date/from (Instant/ofEpochMilli timestamp))})))))
