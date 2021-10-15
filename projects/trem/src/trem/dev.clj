(ns trem.dev
  (:require [io.pedestal.http :as http]
            [trem.core :as trem]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.interceptor.chain :as chain])
  (:import (org.eclipse.jetty.servlet ServletContextHandler)
           (org.eclipse.jetty.server.handler.gzip GzipHandler)))

(defn context-configurator
  [^ServletContextHandler context]
  (let [gzip-handler (GzipHandler.)]
    (.addIncludedMethods gzip-handler (make-array String 0))
    (.setExcludedAgentPatterns gzip-handler (make-array String 0))
    (.setGzipHandler context gzip-handler))
  context)

(defonce *state
  (atom nil))

(defn watch
  [sym args]
  (let [smart-watch (fn [ctx]
                      (let [service-map (apply (requiring-resolve sym) args)
                            routes (trem/expand-draw-routes service-map)
                            interceptors (-> {::http/routes routes}
                                           http/default-interceptors
                                           http/dev-interceptors
                                           ::http/interceptors)]
                        (chain/enqueue ctx interceptors)))]
    (swap! *state (fn [server]
                    (some-> server http/stop)
                    (-> {::http/interceptors      [(interceptor/interceptor {:enter smart-watch})]
                         ::http/type              :jetty
                         ::http/join?             false
                         ::http/container-options {:context-configurator context-configurator}
                         ::http/port              8080}
                      http/default-interceptors
                      http/dev-interceptors
                      http/create-server
                      http/start)))))
