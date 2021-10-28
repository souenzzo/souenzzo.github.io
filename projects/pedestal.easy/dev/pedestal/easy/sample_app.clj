(ns pedestal.easy.sample-app
  (:require [io.pedestal.http :as http]
            [pedestal.easy :as pe]))

(def app
  {::http/routes #{["/" :get (fn [_]
                               (prn :ok!)
                               {:body   "abc12xxx 3"
                                :status 200})
                    :route-name :hello]}
   ::http/port   8080})

(comment
  (pe/watch `app))
