(ns br.com.souenzzo.openapi-connect
  (:require [com.wsscode.pathom3.connect.operation :as pco]
            [clojure.string :as string]
            [clojure.edn :as edn]
            [clojure.core.async :as async]
            [clojure.spec.alpha :as s]))

(defprotocol IFetch
  :extend-via-metadata true
  (fetch [this]))

(s/def ::ns string?)
(s/def ::path string?)
(s/def ::method string?)

(defn ->op-name
  [{::keys [ns path method]}
   {:strs [operationId]}]
  (if operationId
    (symbol ns operationId)
    (symbol ns (string/join ":" (map munge [path method])))))

(s/fdef ->op-name
        :args (s/cat :env (s/keys :req [::ns
                                        ::path
                                        ::method])
                     :operation (s/map-of string? any?))
        :return qualified-symbol?)

(defn response->attribute
  [{::keys [ns]} {:strs [schema]}]
  (some-> schema (get "$ref") (string/split #"/") last (->> (keyword ns))))

(s/fdef response->attribute
        :args (s/cat :env (s/keys :req [::ns])
                     :response (s/map-of string? any?))
        :return (s/nilable qualified-keyword?))


(defn operation->docstring
  [{:strs [summary description]}]
  (string/join "\n" [summary "" description]))


(s/fdef operation->docstring
        :args (s/cat :operation (s/map-of string? any?))
        :return string?)


(defn operation->output
  [env {:strs [responses]}]
  (vec (for [[_status response] responses
             :let [ident (response->attribute env response)]
             :when (keyword? ident)]
         ident)))

(s/fdef operation->output
        :args (s/cat :env (s/keys)
                     :operation (s/map-of string? any?))
        :return (s/coll-of qualified-keyword?
                           :kind vector?))

(defn sync-resolve-fn
  [{::keys [spec path method ns] :as env}]
  (let [{:strs [responses]
         :as   operation} (get-in spec ["paths" path method])
        responses (for [[status response] responses]
                    (assoc response
                      ::target-status (edn/read-string (str status))
                      ::attribute (response->attribute env response)))]
    {::pco/op-name   (->op-name env operation)
     ::pco/docstring (operation->docstring operation)
     ::pco/output    (operation->output env operation)
     ::pco/resolve   (fn [env input]
                       (let [env (assoc env ::operation operation
                                            ::ns ns
                                            ::params input)
                             {::keys [status body]} (fetch env)]
                         (first (for [{::keys [target-status attribute]} responses
                                      :when (= target-status status)]
                                  {attribute body}))))}))
