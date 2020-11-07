(ns br.com.souenzzo.omninotes
  (:require [io.pedestal.http :as http]
            [hiccup2.core :as h]
            [ring.util.mime-type :as mime]))

(defn index
  [req]
  (let [html [:html
              [:head]
              [:body
               [:div "ok!"]]]]
    {:body    (->> html
                   (h/html {:mode :html})
                   (str "<!DOCTYPE html>\n"))
     :headers {"Content-Type" (mime/default-mime-types "html")}
     :status  200}))

(def routes
  `#{["/" :get index]})

(defonce state (atom nil))
(defn -main
  [& _]
  (swap! state (fn [st]
                 (some-> st http/stop)
                 (-> {::http/type   :jetty
                      ::http/join?  true
                      ::http/routes routes
                      ::http/port   8080}

                     http/default-interceptors
                     http/dev-interceptors
                     http/create-server
                     http/start))))
