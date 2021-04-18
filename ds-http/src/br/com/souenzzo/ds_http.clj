(ns br.com.souenzzo.ds-http
  (:require [clojure.string :as string]
            [io.pedestal.http :as http]
            [io.pedestal.http.impl.servlet-interceptor :as servlet-interceptor]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.interceptor.chain :as chain])
  (:import (java.net ServerSocket Socket URI SocketException)
           (java.io InputStream OutputStream Closeable)
           (java.util.concurrent Executors ExecutorService)
           (java.nio.charset StandardCharsets)))

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

(def open-client
  {:name  ::open-clinet
   :enter (fn [{::keys [^Socket client]
                :as    ctx}]
            (assoc ctx
              ::in (.getInputStream client)
              ::out (.getOutputStream client)))
   :leave (fn [{::keys [^Closeable in ^Closeable out]
                :as    ctx}]
            (.close in)
            (.close out)
            ctx)
   :error (fn [{::keys [^Closeable in ^Closeable out]
                :as    ctx}]
            (.close in)
            (.close out)
            ctx)})

(def parse-method
  {:name  ::parse-method
   :enter (fn [{::keys [^InputStream in]
                :as    ctx}]
            (let [method (str (loop [sb (StringBuffer.)
                                     c (.read in)]
                                ;; TODO: max-length?!
                                #_(.length (StringBuffer.))
                                (case c
                                  -1 (throw (ex-info "Unexcpected end-of-file"
                                                     {:cognitect.anomalies/category :cognitect.anomalies/interrupted}))
                                  32 sb
                                  (recur (.append sb (char c))
                                         (.read in)))))]
              (assoc-in ctx [:request :request-method] (keyword (string/lower-case method)))))})

(def parse-path
  {:name  ::parse-path
   :enter (fn [{::keys [^InputStream in]
                :as    ctx}]
            (let [path (str (loop [sb (StringBuffer.)
                                   c (.read in)]
                              ;; TODO: max-length?!
                              #_(.length (StringBuffer.))
                              (case c
                                -1 (throw (ex-info "Unexcpected end-of-file"
                                                   {:cognitect.anomalies/category :cognitect.anomalies/interrupted}))
                                32 sb
                                (recur (.append sb (char c))
                                       (.read in)))))
                  uri (URI/create path)]
              (update ctx :request assoc
                      :uri (.getPath uri)
                      :query-string (.getQuery uri))))})

(def parse-version
  {:name  ::parse-version
   :enter (fn [{::keys [^InputStream in]
                :as    ctx}]
            (let [version (str (loop [sb (StringBuffer.)
                                      c (.read in)]
                                 ;; TODO: max-length?!
                                 #_(.length (StringBuffer.))
                                 (case c
                                   13 (recur sb (.read in))
                                   10 sb
                                   -1 (throw (ex-info "Unexcpected end-of-file"
                                                      {:cognitect.anomalies/category :cognitect.anomalies/interrupted}))
                                   (recur (.append sb (char c))
                                          (.read in)))))]
              (assoc-in ctx [:request :protocol] version)))})


(def parse-headers
  {:name  ::parse-headers
   :enter (fn [{::keys [^InputStream in]
                :as    ctx}]
            (let [headers (persistent! (loop [sb (StringBuffer.)
                                              headers (transient {})
                                              current-key nil
                                              c (.read in)]
                                         ;; TODO: max-length?!
                                         #_(.length (StringBuffer.))
                                         (case c
                                           13 (recur sb
                                                     headers
                                                     current-key
                                                     (.read in))
                                           58 (recur (StringBuffer.)
                                                     headers
                                                     (str sb)
                                                     (.read in))
                                           10 (if current-key
                                                (recur (StringBuffer.)
                                                       (assoc! headers current-key (string/triml (str sb)))
                                                       nil
                                                       (.read in))
                                                headers)
                                           -1 (throw (ex-info "Unexcpected end-of-file"
                                                              {:cognitect.anomalies/category :cognitect.anomalies/interrupted}))
                                           (recur (.append sb (if current-key
                                                                (char c)
                                                                (Character/toLowerCase (char c))))
                                                  headers
                                                  current-key
                                                  (.read in)))))]
              (assoc-in ctx [:request :headers] headers)))})

(def write-body
  {:name  ::write-body
   :leave (fn [{::keys [^OutputStream out]
                :keys  [response]
                :as    ctx}]
            (servlet-interceptor/write-body-to-stream (:body response) out)
            ctx)})

(def write-headers
  {:name  ::write-headers
   :leave (fn [{::keys [^OutputStream out]
                :keys  [response]
                :as    ctx}]
            (doseq [[k v] (:headers response)]
              (.write out (.getBytes (str k ": " v "\r\n")
                                     StandardCharsets/UTF_8)))
            (.write out (.getBytes "\r\n" StandardCharsets/UTF_8))
            ctx)})

(def write-status
  {:name  ::write-status
   :leave (fn [{::keys [^OutputStream out]
                :keys  [response]
                :as    ctx}]
            (let [code (:status response)
                  reason (code->reason code)]
              (.write out (.getBytes (str code " " reason "\r\n")
                                     StandardCharsets/UTF_8)))
            ctx)})

(def write-version
  {:name  ::write-status
   :leave (fn [{::keys [^OutputStream out]
                :as    ctx}]
            (.write out (.getBytes "HTTP/1.1 " StandardCharsets/UTF_8))
            ctx)})


(def base-interceptors
  (map interceptor/interceptor
       [open-client
        write-body
        write-headers
        write-status
        write-version
        parse-method
        parse-path
        parse-version
        parse-headers]))

(defn http:type [{::http/keys [port interceptors]
                  :as         service-map} _]
  (let [*server (delay (ServerSocket. port))
        *thread-pool (delay (Executors/newFixedThreadPool 4))]
    (assoc service-map
      ::start-fn (fn []
                   (let [^ServerSocket server @*server
                         ^ExecutorService thread-pool @*thread-pool
                         ctx (-> service-map
                                 (assoc ::server server
                                        ::thread-pool thread-pool))]
                     (.execute thread-pool
                               (fn accept []
                                 (try
                                   (let [client (.accept server)]
                                     (.execute thread-pool accept)
                                     (-> ctx
                                         (assoc ::client client)
                                         (chain/execute (concat base-interceptors
                                                                interceptors))))
                                   (catch SocketException _ex))))))
      ::stop-fn (fn []
                  (let [^ServerSocket server @*server
                        ^ExecutorService thread-pool @*thread-pool]
                    (.close server)
                    (.shutdown thread-pool))))))

(defn http:chain-provider [{::http/keys [interceptors]
                            :as         service-map}]
  (assoc service-map
    ::http/service-fn (servlet-interceptor/http-interceptor-service-fn
                        interceptors
                        service-map)))
