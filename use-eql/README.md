# use-eql

> An easy library to use EQL API's from reagent or any other react library

# Counter example

Here a counter component.

It should render an button with a number

Every time that you click on the button, it should send a mutation to the server and re-fetch its state.

```clojure
#_(require '[br.com.souenzzo.use-eql :as use-eql])
(defn Counter
  []
  (let [;; fetch returns a conn(ection) object
        ;; it start by sending this query to your API
        conn (use-eql/fetch {::use-eql/query [:app.counter/current-number]})
        ;; if you deref this object, you will get the result
        ;; tree should be something like {:app.counter/current-number 0}
        tree @conn
        {:app.counter/keys [current-number]} tree]
    [:button
     ;; we can send mutations via conn
     ;; it will auto-join the original query on mutation, like this
     ;; [{(app.counter/increment {}) [:app.counter/current-number]}]
     ;; so it should auto-fetch any changes caused by your mutation
     {:on-click #(use-eql/transact conn `[(app.counter/increment {})])}
     current-number]))
```

To use this component, you should use "Functional Components", from reagent like this:

`[:f> Counter]`

Functional components are the way of use react hooks inside reagent.


## Ideas

keep `use-eql` agnostic
```clojure
[use-eql/provider {:value use-eql.pathom/parser}
  [Counter]]
  
[use-eql/provider {:value use-eql.fetch/remote}
  [Counter]]
```
