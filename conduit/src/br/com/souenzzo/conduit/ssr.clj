(ns br.com.souenzzo.conduit.ssr
  (:require [cheshire.core :as json]
            [clojure.instant :as instant]
            [clojure.java.io :as io]
            [com.wsscode.pathom3.connect.indexes :as pci]
            [com.wsscode.pathom3.connect.operation :as pco]
            [com.wsscode.pathom3.interface.smart-map :as psm]
            [com.wsscode.pathom3.interface.eql :as p.eql]
            [hiccup2.core :as h]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [ring.util.mime-type :as mime]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [io.pedestal.http.csrf :as csrf])
  (:import (java.net URI URLEncoder)
           (java.nio.charset StandardCharsets)))

(def ^String utf-8 (str (StandardCharsets/UTF_8)))

(set! *warn-on-reflection* true)

(defn encode-uri-component
  [x]
  (URLEncoder/encode (str x) utf-8))

(defn href
  [route-name & opts]
  (URI. (apply route/url-for route-name opts)))

(defn action
  [{::csrf/keys [anti-forgery-token]} sym]
  {:method "POST"
   :action (href :conduit.api/mutation
                 :params {:sym                                  sym
                          (keyword csrf/anti-forgery-token-str) anti-forgery-token})})

(defn ui-head
  [_]
  [:head
   [:meta {:charset utf-8}]
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
  [_]
  [:nav.navbar.navbar-light
   [:div.container
    [:a.navbar-brand {:href (href :conduit.page/home)}
     "conduit"]
    [:ul.nav.navbar-nav.pull-xs-right
     [:li.nav-item
      [:a.nav-link.active {:href (href :conduit.page/home)}
       "Home"]]
     [:li.nav-item
      [:a.nav-link {:href (href :conduit.page/editor)}
       [:i.ion-compose]
       (str " " "New Post")]]
     [:li.nav-item
      [:a.nav-link {:href (href :conduit.page/settings)}
       [:i.ion-gear-a]
       (str " " "Settings")]]
     [:li.nav-item
      [:a.nav-link {:href (href :conduit.page/register)}
       "Sign up"]]]]])

(defn ui-footer
  [_]
  [:footer
   [:div.container
    [:a.logo-font {:href (href :conduit.page/home)}
     "conduit"]
    [:span.attribution
     "An interactive learning project from"
     [:a {:href "https://thinkster.io"} "Thinkster"]
     ". Code &amp; design licensed under MIT."]]])

(defn ui-register
  [{::csrf/keys [anti-forgery-token]
    :as         req}]
  {:html   [:html
            (ui-head req)
            [:body
             (ui-nav req)
             [:div.auth-page
              [:div.container.page
               [:div.row
                [:div.col-md-6.offset-md-3.col-xs-12
                 [:h1.text-xs-center "Sign up"]
                 [:p.text-xs-center
                  [:a {:href (href :conduit.page/login)}
                   "Have an account?"]]
                 #_[:ul.error-messages
                    [:li "That email is already taken"]]
                 [:form
                  (action req 'conduit.operation/register)
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

(defn ui-home
  [req]
  (let [{:conduit.feed/keys [tags articles]} (psm/smart-map req)]
    {:html   [:html
              (ui-head req)
              [:body
               (ui-nav req)
               [:div.home-page
                [:div.banner
                 [:div.container
                  [:h1.logo-font "conduit"]
                  [:p "A place to share your knowledge."]]]
                [:div.container.page
                 [:div.row
                  [:div.col-md-9
                   [:div.feed-toggle
                    [:ul.nav.nav-pills.outline-active
                     [:li.nav-item
                      [:a.nav-link.disabled {:href (href :conduit.page/home)}
                       "Your Feed"]]
                     [:li.nav-item
                      [:a.nav-link.active {:href (href :conduit.page/home)}
                       "Global Feed"]]]]
                   (for [{:conduit.article/keys [tags title description slug favorites-count
                                                 author created-at]} articles
                         :let [{:conduit.profile/keys [username image]} author]]
                     [:div.article-preview
                      [:div.article-meta
                       [:a {:href (href :conduit.page/profile
                                        :params {:username username})}
                        [:img {:src image}]]
                       [:div.info
                        [:a.author {:href (href :conduit.page/profile
                                                :params {:username username})}
                         username]
                        [:span.date (str created-at)]]
                       [:button.btn.btn-outline-primary.btn-sm.pull-xs-right
                        [:i.ion-heart] favorites-count]]
                      [:a.preview-link {:href (href :conduit.page/article
                                                    :params {:slug slug})}
                       [:h1 title]
                       [:p description]
                       [:span "Read more..."]
                       [:ul.tag-list
                        (for [{:conduit.tag/keys [tag]} tags]
                          [:li.tag-default.tag-pill.tag-outline.ng-binding.ng-scope
                           tag])]]])]
                  [:div.col-md-3
                   [:div.sidebar
                    [:p "Popular Tags"]
                    [:div.tag-list
                     (for [{:conduit.tag/keys [tag]} tags]
                       [:a.tag-pill.tag-default {:href (href :conduit.page/home
                                                             :params {:tag tag})}
                        tag])]]]]]]
               (ui-footer req)]]
     :status 200}))

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
                                     :conduit.article/body            body
                                     :conduit.article/favorited?      favorited
                                     :conduit.article/created-at      (instant/read-instant-date createdAt)
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
  [req]
  {:status  303
   :headers {"Location" "/"}})

(def env
  (pci/register [operation:GetTags operation:GetArticles]))

(def merge-env
  {:name  ::merge-env
   :enter (fn [ctx]
            (update ctx :request merge env))})
(def render-hiccup
  {:name  ::render-hiccup
   :leave (fn [{:keys [response]
                :as   ctx}]
            (if-let [body (:html response)]
              (-> ctx
                  (assoc-in [:response :body] (->> body
                                                   (h/html {:mode :html})
                                                   (str "<!DOCTYPE html>\n")))
                  (assoc-in [:response :headers "Content-Type"] (mime/default-mime-types "html")))
              ctx))})

(def auth
  [(body-params/body-params)
   (middlewares/session)
   (csrf/anti-forgery {:read-token (fn [{:keys [query-params]}]
                                     (get query-params (keyword csrf/anti-forgery-token-str)))})])

(def routes
  #{["/" :get (conj auth merge-env render-hiccup ui-home)
     :route-name :conduit.page/home]
    ["/editor" :get (conj auth merge-env render-hiccup ui-home)
     :route-name :conduit.page/editor]
    ["/settings" :get (conj auth merge-env render-hiccup ui-home)
     :route-name :conduit.page/settings]
    ["/register" :get (conj auth merge-env render-hiccup ui-register)
     :route-name :conduit.page/register]
    ["/login" :get (conj auth merge-env render-hiccup ui-home)
     :route-name :conduit.page/login]
    ["/article/:slug" :get (conj auth merge-env render-hiccup ui-home)
     :route-name :conduit.page/article]
    ["/profile/:username" :get (conj auth merge-env render-hiccup ui-home)
     :route-name :conduit.page/profile]
    ["/api/*sym" :post (conj auth
                             merge-env
                             std-mutation)
     :route-name :conduit.api/mutation]})

(def service
  (-> {::http/join?  false
       ::http/port   8080
       ::http/routes (fn []
                       (route/expand-routes @#'routes))
       ::http/type   :jetty}
      http/default-interceptors
      http/dev-interceptors))


(defonce state (atom nil))

(defn -main
  [& _]
  (swap! state
         (fn [st]
           (some-> st http/stop)
           (-> service
               http/create-server
               http/start))))