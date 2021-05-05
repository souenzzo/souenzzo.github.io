(ns br.com.souenzzo.use-eql.fetch
  (:require [cognitect.transit :as t]
            ["react" :as r]
            [br.com.souenzzo.use-eql :as use-eql]
            [clojure.core.async :as async]))

(def driver
  (reify use-eql/IDriver
    (process [this tx]
      (let [body (t/write (t/writer :json) tx)
            fetch (js/window.fetch "/api"
                    #js{:method "POST"
                        :body   body})
            p (async/promise-chan)]
        (-> fetch
          (.then (fn [response] (.text response)))
          (.then (fn [text]
                   (let [tree (t/read (t/reader :json) text)]
                     (async/put! p tree)))))
        p))))

