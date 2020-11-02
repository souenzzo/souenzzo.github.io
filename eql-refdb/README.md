# eql-refdb

Normalize and merge trees using [EQL](https://edn-query-language.org/) queries

Inspirated by [Fulcro Client Database](http://book.fulcrologic.com/#_fulcro_client_database) format, but relaxed. 

# hacking 

Clone the repo and run `clj -A:cljs:dev -M -m sample.dev-server`

# Examples

```clojure
;; db-before
{}
;; query
[^{:index :todo/id}
 {:app/todos [:todo/id
              :todo/text]}]
;; tree
{:app/todos [{:todo/id   1
              :todo/text "Hello"}
             {:todo/id   2
              :todo/text "World"}]}
;; db-after
{:app/todos [[:todo/id 1]
             [:todo/id 2]]
 :todo/id {1 {:todo/id   1
              :todo/text "Hello"}
           2 {:todo/id   2
              :todo/text "World"}}}
```

# Use-cases

- Manage state in re-frame applications
The `:parser` effect can both `fetch` data from many origins (using pathom) and merge/normalize it results 
into db-state, without write a "success" effect.
```clojure
(rf/reg-fx :parser (...))

(rf/reg-event-fx :my-effect (fn [_ _]
                              {:parser [^{:index :todo/id}
                                        {:app/todos [:todo/id
                                                     :todo/text]}]}))
 
```