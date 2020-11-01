# Flan

> A easy HTTP server for learning clojure


## Get started

- Install [clojure](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)

- Create a empty directory and enter on it.

```bash 
mkdir my-first-webapp
cd my-first-webapp
```

- Create a `deps.edn` file and a `src` dir for sources

```bash 
touch deps.edn
mkdir src
```

- Copy and paste this into `deps.edn`

```clojure
{:deps    {br.com.souenzzo/flang {:git/url "https://github.com/souenzzo/flan"
                                  :sha     "c387d06e36d9f03e0cf12c4fcf5814f7300a00c0"}}
 :aliases {:dev {:main-opts ["-m" "br.com.souenzzo.flan" "--dev" "my-first-webapp"]}}} 
```

- Run it!

```
clj -A:dev
```

- It will start at [localhost:8080](http://localhost:8080). Connect your browser into it and flow the instructions!

## Simple examples

- A simple and incomplete example of "Todo APP"

```clojure

(defonce todos (atom #{}))

(defn -get
  [req]
  [:html
   [:head]
   [:body
    [:form
     {:method "POST"}
     [:input {:name "text"}]
     [:input {:type "submit"}]]
    [:ul
     (for [todo @todos]
       [:li todo])]]])

(defn -post
  [{:keys [params headers]}]
  (swap! todos conj (:text params))
  {:headers {"Location" (get headers "referer" "/")}
   :status  301})

```
