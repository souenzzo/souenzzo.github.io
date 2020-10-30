(ns br.com.souenzzo.ds-http
  (:require [clojure.string :as string])
  (:import (java.net ServerSocket SocketException Socket)
           (java.io InputStream OutputStream)
           (java.util.concurrent Executors)
           (java.lang AutoCloseable)))

(set! *warn-on-reflection* true)

;; from https://tools.ietf.org/html/rfc7231#section-6.1
(comment
  (->> "| 200 | OK | |"
       string/split-lines
       (map string/trim)
       (map #(string/split % #"\|"))
       (map #(map string/trim %))
       (map rest)
       (map (juxt (comp read-string first)
                  second))
       (into (sorted-map))))

(def code->reason
  {100 "Continue"
   101 "Switching Protocols"
   200 "OK"
   201 "Created"
   202 "Accepted"
   203 "Non-Authoritative Information"
   204 "No Content"
   205 "Reset Content"
   206 "Partial Content"
   300 "Multiple Choices"
   301 "Moved Permanently"
   302 "Found"
   303 "See Other"
   304 "Not Modified"
   305 "Use Proxy"
   307 "Temporary Redirect"
   400 "Bad Request"
   401 "Unauthorized"
   402 "Payment Required"
   403 "Forbidden"
   404 "Not Found"
   405 "Method Not Allowed"
   406 "Not Acceptable"
   407 "Proxy Authentication Required"
   408 "Request Timeout"
   409 "Conflict"
   410 "Gone"
   411 "Length Required"
   412 "Precondition Failed"
   413 "Payload Too Large"
   414 "URI Too Long"
   415 "Unsupported Media Type"
   416 "Range Not Satisfiable"
   417 "Expectation Failed"
   426 "Upgrade Required"
   500 "Internal Server Error"
   501 "Not Implemented"
   502 "Bad Gateway"
   503 "Service Unavailable"
   504 "Gateway Timeout"
   505 "HTTP Version Not Supported"})

(defprotocol IInputStream
  (-read [this]))

(extend-protocol IInputStream
  InputStream
  (-read [this] (.read this)))

(defprotocol IOutputStream
  (-write [this b]))

(extend-protocol IOutputStream
  OutputStream
  (-write [this b]
    (.write this (int b))
    this))

(defn read-method
  [is]
  (keyword (string/lower-case (loop [m ""]
                                (let [c (-read is)]
                                  (if (== 32 c)
                                    m
                                    (recur (str m (char c)))))))))


(defn read-path
  [is]
  (loop [m ""]
    (let [c (-read is)]
      (if (== 32 c)
        m
        (recur (str m (char c)))))))

(defn read-protocol
  [is]
  (loop [m ""]
    (let [c (-read is)]
      (if (== 13 c)
        (do
          (-read is)
          m)
        (recur (str m (char c)))))))

(defn read-header-key
  [is]
  (some-> (loop [s nil]
            (let [c (-read is)]
              (if (or (== 58 c)
                      (== 13 c))
                s
                (recur (str s (char c))))))
          string/lower-case))

(defn read-header-value
  [is]
  (loop [s nil]
    (let [c (-read is)]
      (if (== 13 c)
        (do
          (-read is)
          s)
        (recur (str s (char c)))))))

(defn read-headers
  [is]
  (loop [headers (transient {})]
    (if-let [hk (read-header-key is)]
      (recur
        (assoc! headers hk
                (if-let [current (get headers hk)]
                  (str current (if (.equals "cookie" hk)
                                 ";"
                                 ",")
                       (read-header-value is))
                  (read-header-value is))))
      (persistent! headers))))

(defn stop
  [{::keys [stop-fn]
    :as    env}]
  (stop-fn env))

(defn in->request
  [{::keys [in] :as env}]
  (let [method (read-method in)
        path (string/split (read-path in)
                           #"\?")
        protocol (read-protocol in)
        headers (read-headers in)]
    (assoc env
      :ring.request/body in
      :ring.request/headers headers
      :ring.request/method method
      :ring.request/path (first path)
      :ring.request/protocol protocol
      :ring.request/query (last path)
      #_:ring.request/remote-addr
      :ring.request/scheme :http
      #_:ring.request/server-name
      #_:ring.request/ssl-client-cert)))

(defn response->out
  [{::keys              [out]
    :ring.response/keys [status body headers]}]
  (reduce -write out (.getBytes (str "HTTP/1.1 " status " " (code->reason status) "\r\n")))
  (reduce-kv
    (fn [out k vs]
      (doseq [v (if (coll? vs)
                  vs [vs])]
        (reduce -write out (.getBytes (str k ":" v "\r\n")))))
    out headers)
  (-write out (int \return))
  (-write out (int \newline))
  (cond
    (fn? body) (body out)
    (bytes? body) (reduce -write out body)
    :else (reduce -write out (.getBytes (str body)))))

(defprotocol ISocket
  (^AutoCloseable -input-stream [this])
  (^AutoCloseable -output-stream [this]))

(extend-protocol ISocket
  Socket
  (-input-stream [this]
    (.getInputStream this))
  (-output-stream [this]
    (.getOutputStream this)))

(defn process
  [{::keys [handler client] :as env}]
  (with-open [in (-input-stream client)
              out (-output-stream client)]
    (-> env
        (assoc ::in in ::out out)
        in->request
        handler
        (assoc ::out out)
        response->out)))

(defn start
  [{:ring.request/keys [server-port]
    :as                env}]
  (let [thread-pool (Executors/newFixedThreadPool 2)
        server (ServerSocket. server-port)
        env (assoc env ::thread-pool thread-pool
                       ::server server)
        a-server (agent env)]
    (letfn [(stop-fn [_]
              (.shutdown thread-pool)
              (.close server))
            (watch-accept [{::keys [^ServerSocket server]
                            :as    env}]
              (try
                (let [client (.accept server)
                      env (assoc env ::stop-fn stop-fn
                                     ::client client)
                      request (agent env)]
                  (send-via thread-pool request #(try
                                                   (process %)
                                                   (finally
                                                     (.close client))))
                  (send-via thread-pool a-server watch-accept)
                  env)
                (catch SocketException _ex)
                (catch Throwable ex
                  (println ex))))]
      (send-via thread-pool a-server watch-accept)
      (assoc env ::stop-fn stop-fn))))

(comment
  (defonce http-state (atom nil))
  (swap! http-state
         (fn [st]
           (some-> st stop)
           (-> {:ring.request/server-port 8080
                ::handler                 (fn [req]
                                            (tap> req)
                                            ;; (pp/pprint req)
                                            (assoc req
                                              :ring.response/body (.getBytes "ok")
                                              :ring.response/headers {"foo" "42"}
                                              :ring.response/status 200))}
               start))))
