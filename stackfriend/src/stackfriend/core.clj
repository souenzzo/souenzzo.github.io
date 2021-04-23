(ns stackfriend.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [com.rpl.specter :as sp]
            [edamame.core :as eda])
  (:import (java.nio.file Paths)
           (java.io File)))

(defn relative-path
  [^File f]
  (let [d (Paths/get (.getAbsolutePath (io/file "."))
                     (into-array String []))
        f (Paths/get (.getAbsolutePath f)
                     (into-array String []))]
    (str (.relativize d f))))

(defn parse-stack-trace-element
  [ste]
  (let [{:keys [className lineNumber fileName]
         :as   m} (bean ste)
        class-parts (string/split className #"\$")
        sym (apply symbol (map #(Compiler/demunge %)
                               (take 2 class-parts)))
        resource-names (for [ext [".clj" ".cljc" ".cljs" ".java" ".class"]]
                         (str (string/join "/" (string/split (first class-parts)
                                                             #"\."))
                              ext))
        [resource-name resource] (first (remove
                                          (comp nil? last)
                                          (for [resource-name resource-names]
                                            (try
                                              [resource-name (io/resource resource-name)]
                                              (catch Throwable ex)))))
        file (try
               (io/file resource)
               (catch Throwable ex))
        form (when (and resource-name
                        (or
                          (string/ends-with? resource-name "clj")
                          (string/ends-with? resource-name "cljs")
                          (string/ends-with? resource-name "cljc")))
               (first (for [form (eda/parse-string-all (slurp resource)
                                                       {:all          true
                                                        :auto-resolve (fn [x]
                                                                        (if (keyword? x)
                                                                          (namespace sym)
                                                                          (str x)))})

                            :let [{:keys [row end-row]} (meta form)]
                            :when (<= row lineNumber end-row)]
                        form)))
        near-form (sp/select-first
                    (sp/codewalker (fn [x]
                                     (let [{:keys [row end-row]} (meta x)]
                                       (when row
                                         (== lineNumber row)))))
                    form)]
    (-> m
        (dissoc :class)
        (assoc :sym sym
               :form form
               :lang (some-> (re-find #"\..+$" fileName)
                             (subs 1)
                             (keyword))
               :class-parts class-parts
               :near-form near-form
               :resource-name resource-name)
        (cond->
          file (assoc :relative-path (relative-path file))))))

(defn explain-str
  [^Throwable ex]
  (let [[fst & others] (.getStackTrace ex)
        {:keys [relative-path lineNumber near-form]} (parse-stack-trace-element fst)]
    (string/join "\n"
                 (concat
                   (for [{:keys [relative-path sym methodName lineNumber near-form class-parts] :as x} (map parse-stack-trace-element (reverse others))
                         x (if relative-path
                             (if (== 2 (count class-parts))
                               [(str  "Invoked " (pr-str sym) " on line " lineNumber " of file " (pr-str relative-path))]
                               [(str "Evaluated this form inside " (pr-str sym)
                                 (str " on line " lineNumber" file " (pr-str relative-path) ":"))
                                (pr-str near-form)])
                             [(str "On method " (pr-str sym) "#" methodName ", line " lineNumber "")])]
                     x)
                   [(str "On line " lineNumber " of the file " (pr-str relative-path))
                    (str "The form " (pr-str near-form))
                    (str "Throw with the message: " (pr-str (ex-message ex)))
                    (pr-str (ex-data ex))]))))

(defn explain
  [ex]
  (println (explain-str ex)))
