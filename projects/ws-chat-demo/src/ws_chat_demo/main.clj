(ns ws-chat-demo.main
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.jetty.websockets :as ws]
            [io.pedestal.log :as log]
            [clojure.core.async :as async]
            [ring.util.mime-type :as mime]))


(defonce state (atom nil))

(defonce ws-clients
  (atom {}))

(defn context-configurator
  [ctx]
  (reset! ws-clients {})
  (ws/add-ws-endpoints ctx
    {"/ws" {:on-connect (ws/start-ws-connection
                          (fn [ws-session send-ch]
                            (log/info :msg "Connect Message!"
                              :ws-session ws-session
                              :send-ch send-ch)
                            (async/put! send-ch "This will be a text message")
                            (async/go
                              (async/<! (async/timeout 1000))
                              (async/put! send-ch "hello again")
                              (async/<! (async/timeout 1000))
                              (async/put! send-ch "once again"))
                            (swap! ws-clients assoc ws-session send-ch)))
            :on-text    (fn [msg]
                          (log/info :msg "Text Message!"
                            :text msg)
                          (doseq [[ws-session send-ch] @ws-clients]
                            (async/put! send-ch (str "Broadcast: " msg))))
            :on-binary  (fn [payload offset length]
                          (log/info :msg "Binary Message!"
                            :binary payload))
            :on-error   (fn [ex]
                          (log/error :msg "WS Error happened"
                            :exception ex))
            :on-close   (fn [num-code reason-text]
                          (log/info :msg "WS Closed:"
                            :num-code num-code
                            :reason reason-text))}}))

(defn dev-main
  [& _]
  (swap! state
    (fn [st]
      (some-> st http/stop)
      (-> {::http/routes            #{}
           ::http/type              :jetty
           ::http/container-options {:context-configurator context-configurator}
           ::http/port              8080
           ::http/join?             false
           ::http/secure-headers    nil
           ::http/mime-types        mime/default-mime-types
           ::http/resource-path     "public"}
        http/default-interceptors
        http/create-server
        http/start))))

(comment
  (dev-main))


