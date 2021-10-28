(ns pedestal.easy
  (:require [io.pedestal.http :as http]
            [clojure.core.async :as async]
            [io.pedestal.log :as log]))

(defonce *systems
  (atom {}))

(defonce *syms (atom #{}))

(defonce new-var
  (let [c (async/chan)]
    (async/go-loop []
      (let [timeout (async/timeout 1000)
            syms @*syms]
        (doseq [sym syms]
          (async/>! c sym))
        (async/<! timeout))
      (recur))
    (async/thread
      (loop []
        (when-let [sym (async/<!! c)]
          (try
            (let [service-map @(requiring-resolve sym)
                  service-map (assoc service-map
                                ::http/type (::http/type service-map :jetty)
                                ::http/port (::http/port service-map 8080)
                                ::http/routes (::http/routes service-map #{})
                                ::http/join? false)]
              (swap! *systems (fn [systems]
                                (let [{::keys [last-try
                                               active-service-map active-server]} (get systems sym)]
                                  (cond
                                    (= active-service-map service-map) systems
                                    (= service-map last-try) systems
                                    :else (let [old-server (some-> active-server http/stop)
                                                [active-server active-service-map ex] (try
                                                                                        [(-> service-map
                                                                                           http/default-interceptors
                                                                                           http/create-server
                                                                                           http/start)
                                                                                         service-map]
                                                                                        (catch Throwable ex
                                                                                          (log/error :sym sym
                                                                                            :exception ex)
                                                                                          [(http/start old-server)
                                                                                           active-service-map
                                                                                           ex]))]
                                            (assoc systems sym
                                                           (if ex
                                                             {::last-try           service-map
                                                              ::active-service-map active-service-map
                                                              ::active-server      active-server}
                                                             {::active-service-map active-service-map
                                                              ::active-server      active-server}))))))))
            (catch Throwable ex
              (log/error :sym sym :exception ex)))
          (recur))))
    c))

(defn watch
  [sym]
  (requiring-resolve sym)
  (swap! *syms conj sym))

