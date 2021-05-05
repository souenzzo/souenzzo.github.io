(ns br.com.souenzzo.use-eql
  (:require ["react" :as r]
            [cognitect.transit :as t]
            [edn-query-language.core :as eql]
            [clojure.core.async :as async]))

(defprotocol IRemote
  :extend-with-meta true
  (transact [this tx])
  (loading? [this]))

(defprotocol IDriver
  :extend-with-meta true
  (process [this tx]))

(def driver (r/createContext nil))

(defn impl
  [{::keys [query]}]
  (let [[{::keys [current-query
                  sym params]} set-query] (r/useState {::current-query query})
        [fetch? set-fetch?] (r/useState true)
        [load? set-load?] (r/useState false)
        [result set-result] (r/useState nil)
        driver (r/useContext driver)]
    (r/useEffect
      (fn []
        (when fetch?
          (set-load? true)
          (let [tree-chan (process driver (if sym
                                            `[{(~sym ~params) ~current-query}]
                                            current-query))]
            (set-fetch? false)
            (async/go
              (let [tree (async/<! tree-chan)]
                (set-load? false)
                (set-result (if (contains? tree sym)
                              (get tree sym)
                              tree))))))
        (fn []
          (prn :bye))))
    (reify
      IDeref
      (-deref [this]
        result)
      IRemote
      (loading? [this] load?)
      (transact [this tx]
        (let [ast (eql/query->ast tx)
              {:keys [dispatch-key children params]} (-> ast
                                                       :children
                                                       first)]
          (set-query (if (symbol? dispatch-key)
                       {::current-query (if children
                                          (eql/ast->query {:type     :root
                                                           :children children})
                                          current-query)
                        ::sym           dispatch-key
                        ::params        params}
                       {::current-query query}))
          (set-fetch? true))))))
