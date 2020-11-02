# eql-refdb

Normalize and merge trees using [EQL](https://edn-query-language.org/) queries

Inspirated by [Fulcro Client Database](http://book.fulcrologic.com/#_fulcro_client_database) format, but relaxed. 

# hacking 


1. Clone the repo
``bash
$ git clone git@github.com:souenzzo/souenzzo.github.io.git
$ cd souenzzo.github.io/eql-refdb
``

1. Run `npm install` for JS stuff

1. Run `clj -A:cljs:dev -M -m sample.dev-server`
 
1. Wait a bit and connect to [localhost:8080](http://localhost:8080)

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