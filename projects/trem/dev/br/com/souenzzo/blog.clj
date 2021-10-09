(ns br.com.souenzzo.blog
  (:require [trem.core :as trem]
            [clojure.string :as string]
            [com.wsscode.pathom3.connect.operation :as pco]
            [com.wsscode.pathom3.interface.eql :as p.eql]))

(pco/defresolver articles:all [env input]
  {::pco/output [{::articles [:article/id]}]}
  (let [articles (for [i (range 10)]
                   {:article/id i})]
    {::articles (vec articles)}))

(pco/defresolver article:pull [env {:article/keys [id]}]
  {::pco/input  [:article/id]
   ::pco/output [:article/title
                 :article/body]}
  (let [base-body (str "Hello " id " body")]
    {:article/title (str "Title " id)
     :article/body  (string/join "\n"
                      (repeat id base-body))}))


(def articles#index
  {;; app/controllers/articles_controller.rb#index
   ::trem/controller (fn [env]
                       (p.eql/process env [{::articles [:article/id
                                                        :article/title]}]))
   ;; app/views/articles/index.html.erb
   ::trem/view       (fn [{::keys [articles]}]
                       (list
                         [:h1 "Articles"]
                         [:ul
                          (for [{:article/keys [title id]} articles]
                            [:li
                             [:a {:href (trem/path ::article {:id id})}
                              title]])]
                         (trem/link-to "New Article" :new-article-path)))})

(def articles#new
  {;; app/controllers/articles_controller.rb#new
   ::trem/controller (fn [env]
                       (if-let [id (some-> env :path-params :id Long/parseLong)]
                         (p.eql/process env {:article/id id}
                           [:article/id
                            :article/title
                            :article/body])
                         (throw (ex-info (str "Can't find article " (-> env :path-params :id))
                                  {:cognitect.anomalies/category :cognitect.anomalies/not-found}))))
   ;; app/views/articles/new.html.erb
   ::trem/view       (fn [_]
                       (list
                         [:h1 "New Article"]
                         (trem/form-with
                           [:div
                            (trem/form-label :article/title) [:br]
                            (trem/form-text-field :article/title)]
                           [:div
                            (trem/form-label :article/body) [:br]
                            (trem/form-text-area :article/body)]
                           [:div
                            (trem/form-submit)])))})
(def articles#show
  {;; app/controllers/articles_controller.rb#show
   ::trem/controller (fn [env]
                       (if-let [id (some-> env :path-params :id Long/parseLong)]
                         (p.eql/process env {:article/id id}
                           [:article/id
                            :article/title
                            :article/body])
                         (throw (ex-info (str "Can't find article " (-> env :path-params :id))
                                  {:cognitect.anomalies/category :cognitect.anomalies/not-found}))))
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
    (-> {::trem/routes-draw routes
         ::trem/operations  [article:pull
                             articles:all]}
      trem/start)))

(defonce *state (atom nil))

(defn -main
  [& _]
  (swap! *state (fn [st]
                  (some-> st trem/stop)
                  (start))))
