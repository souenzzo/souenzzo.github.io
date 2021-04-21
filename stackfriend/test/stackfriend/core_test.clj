(ns stackfriend.core-test
  (:require [clojure.test :refer [deftest]]
            [midje.sweet :refer [fact =>]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [com.rpl.specter :as sp]
            [edamame.core :as eda]))

(defn stack
  []
  (ex-info "a" {:v 52}))

(defn create-a-stack-trace-element
  []
  (first (.getStackTrace (ex-info "" {}))))

(defn parse-stack-trace-element
  [ste]
  (let [{:keys [className lineNumber]
         :as   m} (bean ste)
        class-parts (string/split className #"\$")
        sym (apply symbol (map #(Compiler/demunge %)
                               (take 2 class-parts)))
        resource-name (str (string/join "/" (string/split (first class-parts)
                                                          #"\."))
                           ".clj")
        form (first (for [form (eda/parse-string-all (slurp (io/resource resource-name))
                                                     {:all true})
                          :let [{:keys [row end-row]} (meta form)]
                          :when (<= row lineNumber end-row)]
                      form))
        near-form (sp/select-first
                    (sp/codewalker (fn [x]
                                     (let [{:keys [row end-row]} (meta x)]
                                       (== lineNumber row))))
                    form)]
    (-> m
        (dissoc :class)
        (assoc :sym sym
               :form form
               :near-form near-form
               :resource-name resource-name))))

(deftest simple
  (let []
    (fact
      (parse-stack-trace-element (create-a-stack-trace-element))
      => {:classLoaderName nil
          :className       "stackfriend.core_test$create_a_stack_trace_element"
          :fileName        "core_test.clj"
          :lineNumber      15
          :form            '(defn
                              create-a-stack-trace-element
                              []
                              (first (.getStackTrace (ex-info "" {}))))
          :methodName      "invokeStatic"
          :moduleName      nil
          :moduleVersion   nil
          :nativeMethod    false
          :near-form       '(first (.getStackTrace (ex-info "" {})))
          :resource-name   "stackfriend/core_test.clj"
          :sym             'stackfriend.core-test/create-a-stack-trace-element})))
