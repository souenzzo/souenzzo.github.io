(ns ws-chat-demo.main
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.jetty.websockets :as ws]
            [io.pedestal.log :as log]
            [clojure.data.json :as json]
            [clojure.core.async :as async]
            [ring.util.mime-type :as mime])
  (:import (org.eclipse.jetty.websocket.api WebSocketConnectionListener WebSocketListener RemoteEndpoint Session)
           (org.eclipse.jetty.servlet ServletHolder ServletContextHandler)
           (javax.servlet Servlet)
           (java.util UUID)))
(set! *warn-on-reflection* true)

(defonce state (atom nil))

(defonce *by-ws-id
  (atom {}))

(defn ws-handler
  [req response]
  (let [ws-id (UUID/randomUUID)]
    (reify
      WebSocketConnectionListener
      (onWebSocketConnect [this ws-session]
        (let [send-ch (async/chan 10)
              remote ^RemoteEndpoint (.getRemote ws-session)]
          (swap! *by-ws-id assoc ws-id {::ws-session ws-session
                                        ::ws-id      ws-id
                                        ::send-ch    send-ch})
          (async/thread
            (loop []
              (when-let [msg (and (.isOpen ws-session)
                               (async/<!! send-ch))]
                (try
                  (ws/ws-send (json/write-str msg)
                    remote)
                  (catch Exception ex
                    (log/error :msg "Failed on ws-send"
                      :exception ex)))
                (recur)))
            (.close ws-session)))
        (log/info :msg "onWebSocketConnect"
          :ws-id ws-id))
      (onWebSocketClose [this status-code reason]
        (log/info :msg "onWebSocketClose"
          :status-code status-code
          :ws-id ws-id
          :reason reason)
        (let [[before after] (swap-vals! *by-ws-id dissoc ws-id)]
          (when-let [ws-session (some->> (get before ws-id) ::ws-session)]
            (.close ^Session ws-session))))
      (onWebSocketError [this cause]
        (log/error :msg "onWebSocketError"
          :ws-id ws-id
          :exception cause))

      WebSocketListener
      (onWebSocketText [this msg-text]
        (let [msg (assoc (json/read-str msg-text
                           :key-fn keyword)
                    :from-ws-id ws-id)]
          (doseq [[_ {::keys [ws-session send-ch]}] @*by-ws-id]
            (async/put! send-ch msg)))
        (log/info :msg "onWebSocketText"
          :ws-id ws-id
          :msg-text msg-text))
      (onWebSocketBinary [this payload offset length]
        (log/info :msg "onWebSocketBinary"
          :binary payload)))))

(defn context-configurator
  [^ServletContextHandler ctx]
  (reset! *by-ws-id {})
  (let [servlet (ws/ws-servlet ws-handler)]
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
