(ns br.com.souenzzo.conduit.ssr
  (:require [io.pedestal.http :as http]
            [hiccup2.core :as h]
            [ring.util.mime-type :as mime]
            [com.wsscode.pathom3.connect.indexes :as pci]
            [com.wsscode.pathom3.connect.operation :as pco]
            [com.wsscode.pathom3.interface.smart-map :as psm]
            [io.pedestal.http.route :as route])
  (:import (java.net URI)
           (java.nio.charset StandardCharsets)))

(def utf-8 (str (StandardCharsets/UTF_8)))

(defn href
  [route-name & opts]
  (URI. (apply route/url-for route-name opts)))

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
    [:a.navbar-brand {:href "index.html"} "conduit"]
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
                      [:a.nav-link.disabled {:href ""}
                       "Your Feed"]]
                     [:li.nav-item
                      [:a.nav-link.active {:href ""}
                       "Global Feed"]]]]
                   (for [{:conduit.profile/keys [username image]
                          :conduit.article/keys [tagList
                                                 title
                                                 description
                                                 slug
                                                 favoritesCount
                                                 favorited
                                                 createdAt]} articles]
                     [:div.article-preview
                      [:div.article-meta
                       [:a {:href "profile.html"}
                        [:img {:src image}]]
                       [:div.info
                        [:a.author {:href (href :conduit.page/profile
                                                :params {:username username})}
                         username]
                        [:span.date (str createdAt)]]
                       [:button.btn.btn-outline-primary.btn-sm.pull-xs-right
                        [:i.ion-heart] favoritesCount]]
                      [:a.preview-link {:href (href :conduit.page/article
                                                    :params {:slug slug})}
                       [:h1 title]
                       [:p description]
                       [:span "Read more..."]
                       [:ul.tag-list
                        (for [tag tagList]
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


(pco/defresolver feed:articles []
  {:conduit.feed/articles [{:conduit.profile/username       "Eric Simons"
                            :conduit.profile/image          "http://i.imgur.com/Qr71crq.jpg"
                            :conduit.article/createdAt      "January 20th"
                            :conduit.article/tagList        ["a" "b"]
                            :conduit.article/favorited      false
                            :conduit.article/favoritesCount 29
                            :conduit.article/title          "How to build webapps that scale"
                            :conduit.article/slug           "1"
                            :conduit.article/description    "This is the description for the post."}]})
(pco/defresolver feed:tags []
  {:conduit.feed/tags [{:conduit.tag/tag "abc"}]})

(def env
  (pci/register [feed:articles
                 feed:tags]))


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

(def routes
  `#{["/" :get [merge-env render-hiccup ui-home]
      :route-name :conduit.page/home]
     ["/editor" :get [merge-env render-hiccup ui-home]
      :route-name :conduit.page/editor]
     ["/settings" :get [merge-env render-hiccup ui-home]
      :route-name :conduit.page/settings]
     ["/register" :get [merge-env render-hiccup ui-home]
      :route-name :conduit.page/register]
     ["/login" :get [merge-env render-hiccup ui-home]
      :route-name :conduit.page/login]
     ["/article/:slug" :get [merge-env render-hiccup ui-home]
      :route-name :conduit.page/article]
     ["/@:username" :get [merge-env render-hiccup ui-home]
      :route-name :conduit.page/profile]})

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