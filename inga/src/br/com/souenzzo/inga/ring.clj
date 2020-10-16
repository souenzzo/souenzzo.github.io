(ns br.com.souenzzo.inga.ring
  (:require [br.com.souenzzo.inga.connect :as ic]
            [hiccup2.core :as h]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]))

(defn on-response
  [request {::keys [body]
            :as    response}]
  (if body
    (assoc response :body (->> (ic/hparser request body)
                               (h/html {:mode :html})
                               (str "<!DOCTYPE html>\n")))
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


(def parser
  (p/parser {::p/plugins [(pc/connect-plugin)]
             ::p/mutate  pc/mutate
             ::p/env     {::p/reader               [p/map-reader
                                                    pc/reader2
                                                    pc/open-ident-reader
                                                    p/env-placeholder-reader]
                          ::p/placeholder-prefixes #{">"}}}))

(def mutate
  {:name  ::mutate
   :enter (fn [{:keys [request]
                :as   ctx}]
            (let [tx `[{(mutate ~{}) [(::body {:pathom/as :body})
                                      (::headers {:pathom/as :headers})
                                      (::status {:pathom/as :status})]}]
                  response (-> (parser request tx)
                               (get `mutate))]
              (assoc ctx :response response)))})