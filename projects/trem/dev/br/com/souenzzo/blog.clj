(ns br.com.souenzzo.blog
  (:require [trem.core :as trem]
            [clojure.string :as string]))

(def articles#index
  {;; app/controllers/articles_controller.rb#index
   ::trem/controller (fn [_]
                       {::articles (for [i (range 10)]
                                     {:article/id    i
                                      :article/title (str (char (+ i (int \a))))})})
   ;; app/views/articles/index.html.erb
   ::trem/view       (fn [{::keys [articles]}]
                       (list
                         [:h1 "Articles"]
                         [:ul
                          (for [{:article/keys [title id]} articles]
                            [:li
                             [:a {:href (trem/path ::article {:id id})}
                              title]])]))})

(def articles#show
  {;; app/controllers/articles_controller.rb#show
   ::trem/controller (fn [{:keys [path-params]}]
                       (let [{:keys [id]} path-params]
                         {:article/id    id
                          :article/title (str "article " id "b")
                          :article/body  (string/join "\n"
                                           (repeat (Long/parseLong id)
                                             "Hello world"))}))
   ;; app/views/articles/show.html.erb
   ::trem/view       (fn [{:article/keys [title body]}]
                       (list
                         [:h1 title]
                         [:pre body]))})

(defn start
  []
  (let [;; config/routes.rb
        routes [;; root "articles#index"
                (assoc articles#index
                  ::trem/route-name ::root
                  :request-method :get
                  :uri "/")
                ;; get "/articles", to: "articles#index"
                (assoc articles#index
                  ::trem/route-name ::articles
                  :request-method :get
                  :uri "/articles")
                ;; get "/articles/:id", to: "articles#show"
                (assoc articles#show
                  ::trem/route-name ::article
                  :request-method :get
                  :uri "/articles/:id")]]
    (-> {::trem/routes-draw routes}
      trem/start)))

(defonce *state (atom nil))

(defn -main
  [& _]
  (swap! *state (fn [st]
                  (some-> st trem/stop)
                  (start))))
