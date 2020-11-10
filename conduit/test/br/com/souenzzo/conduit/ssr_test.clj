(ns br.com.souenzzo.conduit.ssr-test
  (:require [clojure.test :refer [deftest]]
            [br.com.souenzzo.conduit.ssr :as conduit]
            [midje.sweet :refer [fact =>]]
            [io.pedestal.test :refer [response-for]]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [cheshire.core :as json]
            [ring.util.mime-type :as mime]))

(defn ->app
  [{::http/keys [routes]
    :as         service-map}]
  (-> service-map
      http/create-servlet
      (assoc ::route/form-actions (-> routes
                                      route/expand-routes
                                      route/form-action-for-routes))))

(defn request
  [{::http/keys  [service-fn]
    ::route/keys [form-actions]} route-name & opts]
  (let [{:keys [method action]} (apply form-actions route-name opts)
        {:keys [body headers json]} opts
        args (sequence cat (merge (when body
                                    {:body body})
                                  (when headers
                                    {:headers headers})
                                  (when json
                                    {:body    (json/generate-string json)
                                     :headers (assoc headers "Content-Type" (mime/default-mime-types "json"))})))
        {:keys [body]
         :as   response} (apply response-for service-fn (keyword method) action args)]
    (try
      (assoc response :json (json/parse-string body keyword))
      (catch Throwable ex
        response))))

(deftest hello
  (let [app (->app conduit/service)]
    (fact
      (-> (request app ::conduit/index))
      => 200)))
