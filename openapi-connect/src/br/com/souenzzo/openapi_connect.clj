(ns br.com.souenzzo.openapi-connect
  (:require [com.wsscode.pathom3.connect.operation :as pco]
            [clojure.string :as string]))

(defn ->op-name
  [{::keys [ns path method]}
   {:strs [operationId]}]
  (if operationId
    (symbol ns operationId)
    (symbol ns (string/join ":" (map munge [path method])))))

(defn response->attribute
  [{::keys [ns]} {:strs [schema]}]
  (some-> schema (get "$ref") (string/split #"/") last (->> (keyword ns))))

(defn operation->docstring
  [{:strs [summary description]}]
  (string/join "\n" [summary "" description]))

(defn operation->output
  [env {:strs [responses]}]
  (vec (for [[_status {:strs [schema]}] responses
             :let [ident (response->attribute env schema)]
             :when (keyword? ident)]
         ident)))

(defn sync-resolve-fn
  [{::keys [spec path method] :as env}]
  (let [operation (get-in spec ["paths" path method])]
    (pco/resolver
      {::pco/op-name   (->op-name env operation)
       ::pco/docstring (operation->docstring operation)
       ::pco/output    (operation->output env operation)
       ::pco/resolve   (fn [env input]
                         (let []
                           {:ok 42}))})))
