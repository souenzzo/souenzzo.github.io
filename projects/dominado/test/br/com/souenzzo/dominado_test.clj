(ns br.com.souenzzo.dominado-test
  (:require [clojure.test :refer [deftest]]
            [midje.sweet :refer [fact =>]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]))

(defn ip?
  [s]
  (when (string? s)
    (or
      (let [els (string/split s #"\.")]
        (when (< 1 (count els) 5)
          (every? #(and (number? %)
                     (<= 0 % 255)
                     (integer? %))
            (mapv edn/read-string els))))
      (let [els (string/split s #"\>")]
        (when (< 1 (count els) 5)
          (every? #(and (number? %)
                     (<= 0 % 255)
                     (integer? %))
            (mapv edn/read-string els)))))))

(defn parse-hosts
  [x]
  (letfn [(parse-hosts-line [s]
            (merge
              (when-let [[_ text] (re-find #"^\s{0,}\#\s{0,}(.+)" s)]
                (or (try
                      (let [x (parse-hosts-line text)]
                        (when (s/valid? (s/keys :req [::address ::domains])
                                x)
                          (assoc x ::comment true)))
                      (catch Throwable ex))
                  {::comment true
                   ::text    text}))
              (let [[address & domains] (remove string/blank? (string/split s #"\s+"))]
                (when (ip? address)
                  {::address address
                   ::domains (vec domains)}))))]
    (with-open [rdr (io/reader x)]
      (into []
        (map parse-hosts-line)
        (line-seq rdr)))))

(defn bytes-from-lines
  [& lines]
  (.getBytes (string/join "\n" lines)))


(deftest test-parse-hosts
  (let []
    (fact
      (parse-hosts (bytes-from-lines
                     "127.0.0.1 localhost"
                     "# 127.0.0.1 hello"
                     ""
                     "# Hello world"
                     "127.0.0.1 souenzzo.com.br"))
      => [{:br.com.souenzzo.dominado-test/address "127.0.0.1"
           :br.com.souenzzo.dominado-test/domains ["localhost"]}
          {:br.com.souenzzo.dominado-test/address "127.0.0.1"
           :br.com.souenzzo.dominado-test/comment true
           :br.com.souenzzo.dominado-test/domains ["hello"]}
          nil
          {:br.com.souenzzo.dominado-test/comment true
           :br.com.souenzzo.dominado-test/text    "Hello world"}
          {:br.com.souenzzo.dominado-test/address "127.0.0.1"
           :br.com.souenzzo.dominado-test/domains ["souenzzo.com.br"]}])))
