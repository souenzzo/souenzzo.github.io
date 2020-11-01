(ns com.example.todo-list)

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
