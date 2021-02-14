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

(defn href
  [route-name & others]
  (let [{:as opts} others]
    (-> (assoc opts
          :route-name route-name)
        (with-meta `{dvm/-render ~(fn [_ _]
                                    (apply route/url-for route-name others))}))))


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
      :href  (href :conduit.page/home)}
     "conduit"]
    [:ul
     {:class "nav navbar-nav pull-xs-right"}
     (for [[label href] [["Home" (href :conduit.page/home)]
                         ["Sign in" (href :conduit.page/login)]
                         ["Sign up" (href :conduit.page/register)]]]
       [:li
        {:class "nav-item"}
        [:a
         {:class "nav-link"
          :href  href}
         label]])
     #_[:li.nav-item
        [:a.nav-link {:href "#"}
         [:i.ion-compose]
         (str " " "New Post")]]
     #_[:li.nav-item
        [:a.nav-link {:href "#"}
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

(defn ui-account-form
  [req {:keys [fields label tips]}]
  [:div
   {:class "auth-page"}
   [:div
    {:class "container page"}
    [:div
     {:class "row"}
     [:div
      {:class "col-md-6 offset-md-3 col-xs-12"}
      [:h1
       {:class "text-xs-center"}
       label]
      (for [[label href] tips]
        [:p
         {:class "text-xs-center"}
         [:a {:href href}
          label]])
      #_[:ul.error-messages
         [:li "That email is already taken"]]
      [:form
       {}
       (for [field fields]
         [:fieldset
          {:class "form-group"}
          [:input
           (merge {:class "form-control form-control-lg"}
                  field)]])
       [:button
        {:class "btn btn-lg btn-primary pull-xs-right"}
        "Sign in"]]]]]])

(defn ui-register
  [req]
  {:body    (->> [:html
                  [ui-head]
                  [:body
                   [ui-nav]
                   [ui-account-form {:fields [{:type        "text"
                                               :placeholder "Username"}
                                              {:type        "email"
                                               :placeholder "email"}
                                              {:type        "password"
                                               :placeholder "Password"}]
                                     :label  "Sign on"
                                     :tips   {"Have an account?" (href :conduit.page/login)}}]
                   [ui-footer]]]
                 (dvm/render-to-string req)
                 (conj ["<!DOCTYPE html>"])
                 (string/join "\n"))
   :headers {"Content-Type" (mime/default-mime-types "html")}
   :status  200})


(defn ui-login
  [req]
  {:body    (->> [:html
                  [ui-head]
                  [:body
                   [ui-nav]
                   [ui-account-form {:fields [{:type        "text"
                                               :placeholder "Username"}
                                              {:type        "password"
                                               :placeholder "Password"}]
                                     :label  "Sign in"
                                     :tips   {"Need an account?" (href :conduit.page/register)}}]
                   [ui-footer]]]
                 (dvm/render-to-string req)
                 (conj ["<!DOCTYPE html>"])
                 (string/join "\n"))
   :headers {"Content-Type" (mime/default-mime-types "html")}
   :status  200})

(defn ui-body-home
  [env]
  (let [{:conduit.feed/keys [articles tags]} (p.eql/process env [{:conduit.feed/articles [:conduit.article/tags
                                                                                          :conduit.article/title
                                                                                          :conduit.article/description
                                                                                          :conduit.article/slug
                                                                                          :conduit.article/favorites-count
                                                                                          :conduit.article/created-at
                                                                                          {:conduit.article/author [:conduit.profile/username
                                                                                                                    :conduit.profile/image]}]}
                                                                 {:conduit.feed/tags [:conduit.tag/tag]}])]
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
        (for [{:conduit.article/keys [tags title description slug favorites-count author created-at]} articles
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
             tag])]]]]]]))


(defn ui-home
  [req]
  {:body    (->> [:html
                  [ui-head]
                  [:body
                   [ui-nav]
                   [ui-body-home]
                   [ui-footer]]]
                 (dvm/render-to-string req)
                 (conj ["<!DOCTYPE html>"])
                 (string/join "\n"))
   :headers {"Content-Type" (mime/default-mime-types "html")}
   :status  200})

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
                  :route-name :conduit.page/home]
                 ["/login" :get [merge-env ui-login]
                  :route-name :conduit.page/login]
                 ["/register" :get [merge-env ui-register]
                  :route-name :conduit.page/register]}]
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
