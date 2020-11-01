(ns br.com.souenzzo.flan
  (:require [io.pedestal.http :as http]
            [ring.util.mime-type :as mime]
            [hiccup2.core :as h]
            [io.pedestal.http.route :as route]
            [clojure.edn :as edn]
            [clj-kondo.core :as kondo]
            [clojure.tools.cli :as cli]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.body-params :as body-params]
            [clojure.java.io :as io])
  (:import (java.io FileNotFoundException)))

(defn rr
  [ns n]
  (try
    (requiring-resolve (symbol (str ns) (str n)))
    (catch FileNotFoundException ex
      nil)))

(defonce last-error (atom nil))

(defn routes
  [{::keys [ns-name]}]
  (try
    (require (symbol ns-name)
             :reload)
    (prn [(symbol ns-name) :reload :ok])
    (catch Throwable ex))
  (let [{::keys [path]
         :or    {path (str ns-name)}} (rr ns-name "-index")]
    (-> #{["/halp" :get [(fn [req]
                           (let [kondos (for [{:keys [type filename message row col end-row end-col level]} (:findings (kondo/run! {:lint ["src"]}))
                                              :let [content (into [""] (line-seq (io/reader filename)))]]
                                          [:div
                                           [:div message]
                                           [:div level]
                                           [:div filename]
                                           [:div
                                            (for [i (range (- row 3) (+ 4 row))]
                                              [:pre
                                               (when (>= row i
                                                         (or row end-row))
                                                 {:style {:background-color "red"}})
                                               (str i " |")
                                               (get content i)])]])
                                 body [:body
                                       [:header]
                                       [:main
                                        [:h2 "Kondo lints"]
                                        kondos]
                                       [:footer]]]
                             {:body    (->> [:html
                                             [:head
                                              [:title "Halp"]]
                                             body]
                                            (h/html
                                              {:mode :html}
                                              (h/raw "<!DOCTYPE html>\n"))
                                            str)
                              :status  200
                              :headers {"Content-Type" (mime/default-mime-types "html")}}))]
           :route-name ::halp]
          [(str "/" ns-name) :any [{:name  ::error
                                    :error (fn [ctx ex]
                                             (reset! last-error ex)
                                             (let [^Throwable cause (ex-cause ex)]
                                               (assoc ctx
                                                 :response {:body    (->> [:html
                                                                           [:head
                                                                            [:title "Not Found"]]
                                                                           [:body
                                                                            [:header]
                                                                            [:main
                                                                             [:a {:href "/halp"}
                                                                              "Click here for more help"]
                                                                             [:div [:code (ex-message cause)]]
                                                                             [:div [:code (str (class cause))]]]
                                                                            [:footer]]]
                                                                          (h/html
                                                                            {:mode :html}
                                                                            (h/raw "<!DOCTYPE html>\n"))
                                                                          str)
                                                            :headers {"Content-Type" (mime/default-mime-types "html")}
                                                            :status  500})))}
                                   (body-params/body-params)
                                   {:name  ::merge-params
                                    :enter (fn [ctx]
                                             (let [params (-> ctx
                                                              :request
                                                              ((juxt :json-params :form-params :transit-params)))]
                                               (assoc-in ctx [:request :params] (apply merge params))))}

                                   {:name  ::->html
                                    :leave (fn [{:keys [response]
                                                 :as   ctx}]
                                             (if (and (coll? response)
                                                      (= :html (first response)))
                                               (assoc ctx
                                                 :response {:body    (->> response
                                                                          (h/html
                                                                            {:mode :html}
                                                                            (h/raw "<!DOCTYPE html>\n"))
                                                                          str)
                                                            :headers {"Content-Type" (mime/default-mime-types "html")}
                                                            :status  200})
                                               ctx))}
                                   {:name  (keyword ns-name "-handler")
                                    :enter (fn [{{:keys [request-method]} :request
                                                 :keys                    [request]
                                                 :as                      ctx}]
                                             (when-let [-handler (rr ns-name (str "-" (name request-method)))]
                                               (assoc ctx :response (-handler request))))}]]
          :route-name (keyword ns-name)}
        route/expand-routes)))


(defn not-found
  [{::keys [ns-name]}]
  (->> (fn [{:keys [response request]
             :as   ctx}]
         (if (http/response? response)
           ctx
           (let [base-file-name (subs (#'clojure.core/root-resource (symbol ns-name))
                                      1)
                 absolute-name (first (for [suffix ["clj" "cljc"]
                                            :let [r (io/resource (str base-file-name "." suffix))]
                                            :when r]
                                        (.getAbsolutePath (io/file r))))
                 body (->> [:html
                            [:head
                             [:title "Not Found"]]
                            [:body
                             [:header]
                             [:main
                              [:p "Can't find " [:code (-> request :uri pr-str)]]
                              (cond
                                (rr ns-name "-get")
                                [:p "You can try " [:a {:href (str "/" ns-name)}
                                                    (str "/" ns-name)]]
                                absolute-name
                                (list
                                  [:p "There is no function " [:code "-get"] " defined in your main namespace " [:code ns-name]]
                                  [:p "The code for " [:code ns-name] "in on" [:code absolute-name]]
                                  [:p "You can copy and paste this and the end of the file and try again"]
                                  [:pre (format "(ns %s)

(defn -get\n  [req]\n  [:html\n   [:head]\n   [:body\n    \"Hello World!\"]])" ns-name)])
                                :else (list [:p "You need to create a file in " [:code (str "src/" base-file-name ".clj")]]
                                            [:p "The contents of this file should be something like"]
                                            [:pre (format "(ns %s)\n\n(defn -get\n  [req]\n  [:html\n   [:head\n    [:title \"Hello\"]]\n   [:body\n    \"world!\"]])\n"
                                                          ns-name)]))
                              [:a {:href "/halp"}
                               "Click here for more help"]]
                             [:footer]]]
                           (h/html
                             {:mode :html}
                             (h/raw "<!DOCTYPE html>\n"))
                           str)
                 response {:body    body
                           :headers {"Content-Type" (mime/default-mime-types "html")}
                           :status  404}]
             (assoc ctx :response response))))
       (hash-map :name ::not-found :leave)
       interceptor/interceptor))


(def option-spec
  [["-p" "--port PORT" "Port number"
    :default 8080
    :parse-fn edn/read-string
    :validate [number? "Must be a number"
               #(< 1024 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-d" "--dev NS-NAME" "main namespace"
    :validate [string?]]])

(defn start
  [port->server args]
  (let [{{:keys [port dev]} :options
         :keys              [errors]} (cli/parse-opts
                                        args
                                        option-spec)
        errors (concat errors
                       (try
                         (when-not (simple-symbol? (edn/read-string dev))
                           [(str "The name '" dev "' isn't a clojure symbol")])
                         (catch Throwable ex
                           [(str "Clojure can't read '" dev "' as a symbol")])))

        env {::ns-name dev}
        _ (when-not (empty? errors)
            (binding [*out* *err*]
              (run! println errors))
            (System/exit 1))
        _ (some-> port->server
                  (get port)
                  http/stop)
        ret (assoc port->server
              port (-> {::http/routes                (fn []
                                                       (routes env))
                        ::http/join?                 false
                        ::http/mime-types            mime/default-mime-types
                        ::http/port                  port
                        ::http/not-found-interceptor (not-found env)
                        ::http/type                  :jetty}
                       http/default-interceptors
                       http/dev-interceptors
                       http/create-server
                       http/start))]
    (printf "Started at http://localhost:%s\n" port)
    ret))

(defonce port->server (atom {}))

(defn -main
  [& opts]
  (swap! port->server start opts))
