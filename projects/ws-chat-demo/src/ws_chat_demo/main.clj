(ns ws-chat-demo.main
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.jetty.websockets :as ws]
            [io.pedestal.log :as log]
            [clojure.core.async :as async]
            [ring.util.mime-type :as mime])
  (:import (org.eclipse.jetty.websocket.api WebSocketConnectionListener WebSocketListener RemoteEndpoint)
           (org.eclipse.jetty.servlet ServletHolder)
           (javax.servlet Servlet)))


(defonce state (atom nil))

(defonce ws-clients
  (atom {}))

(defn context-configurator
  [ctx]
  (reset! ws-clients {})
  (let [servlet (ws/ws-servlet (fn [req response]
                                 (let [*ws-session (promise)]
                                   (reify
                                     WebSocketConnectionListener
                                     (onWebSocketConnect [this ws-session]
                                       (deliver *ws-session ws-session)
                                       (let [send-ch (async/chan 10)
                                             remote ^RemoteEndpoint (.getRemote ws-session)]
                                         ;; Let's process sends...
                                         (async/thread
                                           (loop []
                                             (when-let [out-msg (and (.isOpen ws-session)
                                                                  (async/<!! send-ch))]
                                               (try
                                                 (ws/ws-send out-msg remote)
                                                 (catch Exception ex
                                                   (log/error :msg "Failed on ws-send"
                                                     :exception ex)))
                                               (recur)))
                                           (.close ws-session))
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
                                     (onWebSocketClose [this status-code reason]
                                       (log/info :msg "WS Closed:"
                                         :status-code status-code
                                         :reason reason))
                                     (onWebSocketError [this cause]
                                       (log/error :msg "WS Error happened"
                                         :exception cause))

                                     WebSocketListener
                                     (onWebSocketText [this msg]
                                       (log/info :msg "Text Message!"
                                         :text msg)
                                       (doseq [[ws-session send-ch] @ws-clients]
                                         (async/put! send-ch (str "Broadcast: " (hash this) msg))))
                                     (onWebSocketBinary [this payload offset length]
                                       (log/info :msg "Binary Message!"
                                         :binary payload))))))]
    (.addServlet ctx (ServletHolder. ^Servlet servlet) "/ws")))


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
        http/start)))
  nil)

(comment
  (dev-main))


