(ns stackfriend.core-test
  (:require [clojure.test :refer [deftest]]
            [midje.sweet :refer [fact =>]]
            [stackfriend.helper :as sfh]
            [stackfriend.core :as sf]))

(defn ^StackTraceElement create-a-stack-trace-element
  []
  (first (.getStackTrace (ex-info "" {}))))

(deftest simple
  (let [ste (create-a-stack-trace-element)]
    (fact
      (dissoc  (sf/parse-stack-trace-element ste) :class-parts :lang)
      => {:classLoaderName nil
          :className       "stackfriend.core_test$create_a_stack_trace_element"
          :fileName        "core_test.clj"
          :lineNumber      9
          :form            '(defn
                              create-a-stack-trace-element
                              []
                              (first (.getStackTrace (ex-info "" {}))))
          :methodName      "invokeStatic"
          :moduleName      nil
          :moduleVersion   nil
          :relative-path   "test/stackfriend/core_test.clj"
          :nativeMethod    false
          :near-form       '(first (.getStackTrace (ex-info "" {})))
          :resource-name   "stackfriend/core_test.clj"
          :sym             'stackfriend.core-test/create-a-stack-trace-element})
    (fact
      (mapv #(dissoc % :class-parts :lang)
            (mapv sf/parse-stack-trace-element (.getStackTrace (sfh/simple-exception))))
      => '[{:relative-path   "test/stackfriend/helper.clj"
            :near-form       (ex-info "" {})
            :classLoaderName nil
            :fileName        "helper.clj"
            :resource-name   "stackfriend/helper.clj"
            :moduleVersion   nil
            :nativeMethod    false
            :className       "stackfriend.helper$create_exception"
            :sym             stackfriend.helper/create-exception
            :moduleName      nil
            :form            (defn create-exception []
                               (ex-info "" {}))
            :lineNumber      6
            :methodName      "invokeStatic"}
           {:relative-path   "test/stackfriend/helper.clj"
            :near-form       (defn create-exception []
                               (ex-info "" {}))
            :classLoaderName nil
            :fileName        "helper.clj"
            :resource-name   "stackfriend/helper.clj"
            :moduleVersion   nil
            :nativeMethod    false
            :className       "stackfriend.helper$create_exception"
            :sym             stackfriend.helper/create-exception
            :moduleName      nil
            :form            (defn create-exception [] (ex-info "" {}))
            :lineNumber      4
            :methodName      "invoke"}
           {:relative-path   "test/stackfriend/helper.clj"
            :near-form       (let [x (create-exception)] (deliver p x))
            :classLoaderName nil
            :fileName        "helper.clj",
            :resource-name   "stackfriend/helper.clj"
            :moduleVersion   nil
            :nativeMethod    false
            :className       "stackfriend.helper$simple_exception$fn__20261"
            :sym             stackfriend.helper/simple-exception
            :moduleName      nil
            :form            (defn simple-exception
                               []
                               (let [p (promise)]
                                 (.start (Thread. (fn []
                                                    (let [x (create-exception)]
                                                      (deliver p x)))))
                                 (clojure.core/deref p)))
            :lineNumber      12
            :methodName      "invoke"}
           {:near-form       nil
            :classLoaderName "app"
            :fileName        "AFn.java"
            :resource-name   "clojure/lang/AFn.class"
            :moduleVersion   nil
            :nativeMethod    false
            :className       "clojure.lang.AFn"
            :sym             clojure.lang.AFn
            :moduleName      nil
            :form            nil
            :lineNumber      22
            :methodName      "run"}
           {:near-form       nil
            :classLoaderName nil
            :fileName        "Thread.java"
            :resource-name   "java/lang/Thread.class"
            :moduleVersion   "16.0.1"
            :nativeMethod    false
            :className       "java.lang.Thread"
            :sym             java.lang.Thread
            :moduleName      "java.base"
            :form            nil
            :lineNumber      831
            :methodName      "run"}])
    (sf/explain (sfh/simple-exception))))
