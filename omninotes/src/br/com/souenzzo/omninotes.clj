(ns br.com.souenzzo.omninotes
  (:require [io.pedestal.http :as http]
            [hiccup2.core :as h]
            [next.jdbc :as j]
            [ring.util.mime-type :as mime]
            [cheshire.core :as json]
            [clojure.spec.alpha :as s])
  (:import (org.postgresql.util PSQLException)))

;; docker run --rm -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:alpine

(def db-spec
  {:dbtype   "postgresql"
   :dbname   "omninotes"
   :host     "localhost"
   :user     "postgres"
   :password "postgres"})

(def telegram-api
  (str "https://api.telegram.org/bot" (System/getenv "TELEGRAM_TOKEN") "/"))

(comment
  (-> telegram-api
      (str "getUpdates")
      slurp
      (json/parse-string true))
  => {:ok     true
      :result [{:update_id 1
                :message   {:message_id 2
                            :from       {:id            3
                                         :is_bot        false
                                         :first_name    "Enzzo"
                                         :username      "souenzzo"
                                         :language_code "en"}
                            :chat       {:id         4
                                         :first_name "Enzzo"
                                         :username   "souenzzo"
                                         :type       "private"}
                            :date       1604790763
                            :text       "Fazer natação as 6"}}]})

(defn save-telegram-message
  [env {:keys [message_id from chat date text]}]
  (j/execute! db-spec
              ["INSERT INTO app_notes()
                VALUES ()"
               message_id from chat date text]))

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

(defn delete-database
  []
  (j/execute! (dissoc db-spec :dbname)
              [(str "DROP DATABASE "
                    (:dbname db-spec))]
              {:isolation :none}))

(defonce state (atom nil))
(defn -main
  [& _]
  ;; create db
  (try
    (j/execute! (dissoc db-spec :dbname)
                [(str "CREATE DATABASE "
                      (:dbname db-spec))]
                {:isolation :none})
    (catch PSQLException ex
      (println (ex-message ex))))
  (try
    (j/execute! db-spec ["CREATE TABLE app_foo ()"])
    (catch PSQLException ex
      (println (ex-message ex))))
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
