(ns br.com.souenzzo.use-eql
  (:require ["react" :as r]
            [cognitect.transit :as t]
            [edn-query-language.core :as eql]))

(defn fetch
  [{::keys [query]}]
  (let [[{::keys [current-query
                  sym params]} set-query] (r/useState {::current-query query})
        [fetch? set-fetch?] (r/useState true)
        [result set-result] (r/useState nil)]
    (r/useEffect
      (fn []
        (when fetch?
          (let [body (-> (t/writer :json)
                         (t/write (if sym
                                    `[{(~sym ~params) ~current-query}]
                                    current-query)))
                fetch (js/fetch "/api"
                                #js{:method "POST"
                                    :body   body})]
            (set-fetch? false)
            (-> fetch
                (.then (fn [x] (.text x)))
                (.then (fn [x]
                         (let [v (-> (t/reader :json)
                                     (t/read x))]
                           (set-result (if (contains? v sym)
                                         (get v sym)
                                         v))))))))
        (fn []
          (prn :bye))))
    [result (fn [query]
              (let [ast (eql/query->ast query)
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
                (set-fetch? true)))]))
