(ns br.com.souenzzo.inga.ring
  (:require [br.com.souenzzo.inga.connect :as ic]
            [hiccup2.core :as h]))

(defn on-response
  [request {::keys [body]
            :as    response}]
  (if body
    (->> (ic/hparser request body)
         (h/html {:mode :html})
         (str "<!DOCTYPE html>\n"))
    response))

(defn interceptor
  [env]
  {:name  ::connect
   :enter (fn [ctx]
            (update ctx :request merge env))
   :leave (fn [{:keys [request]
                :as   ctx}]
            (update ctx :response (partial on-response request)))})

(defn middleware
  "That doggo meme: have no idea what i'm doing"
  [env]
  (fn [handler]
    (fn [request]
      (let [env (merge request env)]
        (->> (handler env)
             (on-response env))))))
