(ns br.com.souenzzo.conduit.ssr
  (:require [io.pedestal.http :as http]))

(defn index
  [req]
  {:status 200})

(def routes
  `#{["/" :get index]})

(def service
  (-> {::http/join?  false
       ::http/port   8080
       ::http/routes routes
       ::http/type   :jetty}
      http/default-interceptors))


(defonce state (atom nil))

(defn -main
  [& _]
  (swap! state
         (fn [st]
           (some-> st http/stop)
           (-> service
               http/create-server
               http/start))))