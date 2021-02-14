(ns br.com.souenzzo.conduit.ssr
  (:require [cheshire.core :as json]
            [clojure.instant :as instant]
            [clojure.java.io :as io]
            [com.wsscode.pathom3.connect.indexes :as pci]
            [com.wsscode.pathom3.connect.operation :as pco]
            [com.wsscode.pathom3.interface.eql :as p.eql]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [br.com.souenzzo.dvm :as dvm]
            [clojure.string :as string]
            [ring.util.mime-type :as mime])
  (:import (java.nio.charset StandardCharsets)))

(defn ui-head
  [_]
  [:head
   [:meta {:charset (str StandardCharsets/UTF_8)}]
   [:link {:rel "icon" :href "data:"}]
   [:title "Conduit"]
   [:link {:href "https://code.ionicframework.com/ionicons/2.0.1/css/ionicons.min.css"
           :rel  "stylesheet"
           :type "text/css"}]
   [:link {:href "https://fonts.googleapis.com/css?family=Titillium+Web:700|Source+Serif+Pro:400,700|Merriweather+Sans:400,700|Source+Sans+Pro:400,300,600,700,300italic,400italic,600italic,700italic"
           :rel  "stylesheet"
           :type "text/css"}]
   [:link {:rel  "stylesheet"
           :href "https://demo.productionready.io/main.css"}]])

(defn ui-nav
  [req]
  [:nav
   {:class "navbar navbar-light"}
   [:div
    {:class "container"}
    [:a
     {:class "navbar-brand"
      :href  "#"}
     "conduit"]
    [:ul
     {:class "nav navbar-nav pull-xs-right"}
     (for [[label route-name] [["Home" :conduit.page/home]
                               ["Sign in" :conduit.page/login]
                               ["Sign up" :conduit.page/register]]]
       [:li
        {:class "nav-item"}
        [:a
         {:class "nav-link"
          :href  "#"}
         label]])
     #_[:li.nav-item
        [:a.nav-link {:href (hiete/href :conduit.page/editor)}
         [:i.ion-compose]
         (str " " "New Post")]]
     #_[:li.nav-item
        [:a.nav-link {:href (hiete/href :conduit.page/settings)}
         [:i.ion-gear-a]
         (str " " "Settings")]]]]])

(defn ui-footer
  [_]
  [:footer
   [:div
    {:class "container"}
    [:a
     {:class "logo-font"
      :href  "#"}
     "conduit"]
    [:span
     {:class "attribution"}
     "An interactive learning project from"
     [:a {:href "https://thinkster.io"}
      "Thinkster"]
     ". Code &amp; design licensed under MIT."]]])

(defn ui-register
  [req]
  {:html   [:html
            [ui-head]
            [:body
             (ui-nav req)
             [:div.auth-page
              [:div.container.page
               [:div.row
                [:div.col-md-6.offset-md-3.col-xs-12
                 [:h1.text-xs-center "Sign up"]
                 [:p.text-xs-center
                  [:a {:href (hiete/href :conduit.page/login)}
                   "Have an account?"]]
                 #_[:ul.error-messages
                    [:li "That email is already taken"]]
                 [:form
                  (hiete/mutation req 'conduit.operation/register)
                  [:fieldset.form-group
                   [:input.form-control.form-control-lg
                    {:type        "text"
                     :placeholder "Your Name"}]]
                  [:fieldset.form-group
                   [:input.form-control.form-control-lg
                    {:type        "text"
                     :placeholder "Email"}]]
                  [:fieldset.form-group
                   [:input.form-control.form-control-lg
                    {:type        "password"
                     :placeholder "Password"}]]
                  [:button.btn.btn-lg.btn-primary.pull-xs-right "Sign up"]]]]]]
             (ui-footer req)]]
   :status 200})


(defn ui-login
  [req]
  {:html   [:html
            (ui-head req)
            [:body
             (ui-nav req)
             [:div.auth-page
              [:div.container.page
               [:div.row
                [:div.col-md-6.offset-md-3.col-xs-12
                 [:h1.text-xs-center "Sign in"]
                 [:p.text-xs-center
                  [:a {:href (hiete/href :conduit.page/register)}
                   "Need an account?"]]
                 #_[:ul.error-messages
                    [:li "That email is already taken"]]
                 [:form
                  (hiete/mutation req 'conduit.operation/login)
                  [:fieldset.form-group
                   [:input.form-control.form-control-lg
                    {:type        "text"
                     :placeholder "Email"}]]
                  [:fieldset.form-group
                   [:input.form-control.form-control-lg
                    {:type        "password"
                     :placeholder "Password"}]]
                  [:button.btn.btn-lg.btn-primary.pull-xs-right "Sign in"]]]]]]
             (ui-footer req)]]
   :status 200})


(defn ui-home
  [req]
  (let [{:conduit.feed/keys [tags articles]} {}
        ui [:html
            [ui-head]
            [:body
             [ui-nav]
             [:div
              {:class "home-page"}
              [:div
               {:class "banner"}
               [:div
                {:class "container"}
                [:h1
                 {:class "logo-font"}
                 "conduit"]
                [:p "A place to share your knowledge."]]]
              [:div
               {:class "container page"}
               [:div
                {:class "row"}
                [:div
                 {:class "col-md-9"}
                 [:div
                  {:class "feed-toggle"}
                  [:ul
                   {:class "nav nav-pills outline-active"}
                   [:li
                    {:class "nav-item"}
                    [:a
                     {:class " nav-link disabled"
                      :href  "#"}
                     "Your Feed"]]
                   [:li
                    {:class "nav-item"}
                    [:a
                     {:class "nav-link active"
                      :href  "#"}
                     "Global Feed"]]]]
                 (for [{:conduit.article/keys [tags title description slug favorites-count
                                               author created-at]} articles
                       :let [{:conduit.profile/keys [username image]} author]]
                   [:div
                    {:class "article-preview"}
                    [:div
                     {:class "article-meta"}
                     [:a {:href "#"}
                      [:img {:src image}]]
                     [:div
                      {:class "info"}
                      [:a
                       {:class "author"
                        :href  "#"}
                       username]
                      [:span
                       {:class "date"}
                       (str created-at)]]
                     [:button
                      {:class "btn btn-outline-primary btn-sm pull-xs-right"}
                      [:i
                       {:class "ion-heart"}]
                      favorites-count]]
                    [:a
                     {:class "preview-link"
                      :href  "#"}
                     [:h1 title]
                     [:p description]
                     [:span "Read more..."]
                     [:ul
                      {:class "tag-list"}
                      (for [{:conduit.tag/keys [tag]} tags]
                        [:li
                         {:class "tag-default tag-pill tag-outline ng-binding ng-scope"}
                         tag])]]])]
                [:div
                 {:class "col-md-3"}
                 [:div
                  {:class "sidebar"}
                  [:p "Popular Tags"]
                  [:div
                   {:class "tag-list"}
                   (for [{:conduit.tag/keys [tag]} tags]
                     [:a
                      {:class "tag-pill tag-default"
                       :href  "#"}
                      tag])]]]]]]
             [ui-footer]]]
        html (dvm/render-to-string req ui)]
    {:body    (string/join "\n" ["<!DOCTYPE html>"
                                 html])
     :headers {"Content-Type" (mime/default-mime-types "html")}
     :status  200}))

(pco/defresolver operation:GetArticles []
  {::pco/output [{:conduit.feed/articles [{:conduit.article/author [:conduit.profile/username
                                                                    :conduit.profile/bio
                                                                    :conduit.profile/image
                                                                    :conduit.profile/following?]}
                                          {:conduit.article/tags [:conduit.tag/tag]}
                                          :conduit.article/body
                                          :conduit.article/created-at
                                          :conduit.article/description
                                          :conduit.article/favorited?
                                          :conduit.article/favorites-count
                                          :conduit.article/slug
                                          :conduit.article/title
                                          :conduit.article/updated-at]}
                 :conduit.feed/articles-count]}
  (let [{:keys [articlesCount articles]} (-> "https://conduit.productionready.io/api/articles"
                                             io/reader
                                             (json/parse-stream true))]
    {:conduit.feed/articles       (for [{:keys [description slug updatedAt createdAt title author favoritesCount body favorited tagList]} articles]
                                    {:conduit.article/slug            slug
                                     :conduit.article/description     description
                                     :conduit.article/title           title
                                     :conduit.article/favorites-count favoritesCount
                                     :conduit.article/updated-at      (instant/read-instant-date updatedAt)
                                     :conduit.article/created-at      (instant/read-instant-date createdAt)
                                     :conduit.article/body            body
                                     :conduit.article/favorited?      favorited
                                     :conduit.article/author          (let [{:keys [username bio image following]} author]
                                                                        {:conduit.profile/username   username
                                                                         :conduit.profile/bio        bio
                                                                         :conduit.profile/image      image
                                                                         :conduit.profile/following? following})
                                     :conduit.article/tags            (for [tag tagList]
                                                                        {:conduit.tag/tag tag})})
     :conduit.feed/articles-count articlesCount}))

(pco/defresolver operation:GetTags []
  {::pco/output [{:conduit.feed/tags [:conduit.tag/tag]}]}
  (let [{:keys [tags]} (-> "https://conduit.productionready.io/api/tags"
                           io/reader
                           (json/parse-stream true))]
    {:conduit.feed/tags (for [tag tags]
                          {:conduit.tag/tag tag})}))

(defn std-mutation
  [{:keys []
    :as   env}]
  (let [tx []
        result (p.eql/process env tx)]
    {:status  303
     :headers {"Location" "/"}}))

(pco/defresolver routes [{::keys [operations]}]
  {::pco/output [::routes]}
  (let [idx (pci/register operations)
        merge-env {:name  ::merge-env
                   :enter (fn [ctx]
                            (update ctx :request merge idx))}
        routes #{["/" :get [merge-env ui-home]
                  :route-name :conduit.page/home]}]
    {::routes routes}))

(pco/defresolver service [{::keys [operations]}]
  {::pco/output [::service]}
  (let [routes (fn []
                 (-> (pci/register operations)
                     (p.eql/process [::routes])
                     ::routes
                     route/expand-routes))]
    {::service (-> {::http/routes routes}
                   http/default-interceptors
                   http/dev-interceptors)}))


(defn operations
  []
  [service
   routes
   operation:GetTags
   operation:GetArticles
   (pco/resolver `operations
                 {::pco/output [::operations]}
                 (fn [_ _]
                   {::operations (operations)}))])

(defonce -env (atom nil))

(defonce state (atom nil))

(defn -main
  [& _]
  (swap! state
         (fn [st]
           (some-> st http/stop)
           (-> (reset! -env (pci/register (operations)))
               (p.eql/process [::service])
               ::service
               (assoc ::http/join? false
                      ::http/port 8080
                      ::http/type :jetty)
               http/create-server
               http/start))))
