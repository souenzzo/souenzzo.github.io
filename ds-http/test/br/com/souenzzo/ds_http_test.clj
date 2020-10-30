(ns br.com.souenzzo.ds-http-test
  (:require [br.com.souenzzo.ds-http :as ds]
            [clj-kondo.core :as kondo]
            [br.com.souenzzo.ds-http.testing :refer [str->is request->input-stream]]
            [clojure.test :refer [deftest is testing]])
  (:import (java.io ByteArrayOutputStream)))

(set! *warn-on-reflection* true)

(deftest unit
  (is (= :get
         (ds/read-method (str->is "GET "))))
  (is (= "/foo"
         (ds/read-path (str->is "/foo "))))
  (is (= "HTTP/1.1"
         (ds/read-protocol (str->is "HTTP/1.1\r\n"))))
  (is (= "host"
         (ds/read-header-key (str->is "Host:"))))
  (is (= "souenzzo.com.br"
         (ds/read-header-value (str->is "souenzzo.com.br\r\n")))))

(deftest ring-headers
  "** :ring.request/headers **
     A Clojure map of lowercased header name strings to corresponding header value strings.
     Where there are multiple headers with the same name, the adapter must concatenate the values into a single string,
   using the ASCII , character as a delimiter.
     The exception to this is the cookie header, which should instead use the ASCII ; character as a delimiter."
  (testing
    "Simple"
    (is (= {"foo" " bar" "car" "tar"}
           (ds/read-headers (str->is "foo: bar\r\ncar:tar\r\n\r\n")))))
  (testing
    "Lower Cases"
    (is (= {"car" "tar" "foo" " bAr"}
           (ds/read-headers (str->is "Foo: bAr\r\ncaR:tar\r\n\r\n")))))
  (testing
    "Multiple headers"
    (is (= {"foo" " bAr,tar"}
           (ds/read-headers (str->is "Foo: bAr\r\nfoo:tar\r\n\r\n")))))
  (testing
    "Multiple cookie headers"
    (is (= {"cookie" " bAr;tar"}
           (ds/read-headers (str->is "cookie: bAr\r\ncookie:tar\r\n\r\n"))))))

(deftest full-request
  (is (= {:ring.request/headers  {"foo" "bar"}
          :ring.request/method   :get
          :ring.request/path     "/foo"
          :ring.request/protocol "HTTP/1.1"
          :ring.request/query    "query"
          :ring.request/scheme   :http}
         (-> (ds/in->request {::ds/in (str->is "GET /foo?query HTTP/1.1\r\nfoo:bar\r\n\r\ncar")})
             (dissoc ::ds/in :ring.request/body)))))

(deftest full-response
  (let [baos (ByteArrayOutputStream.)]
    (ds/response->out
      {:ring.response/status  200
       :ring.response/body    "ok"
       :ring.response/headers {"a" "b"}
       ::ds/out               baos})
    (is (= "HTTP/1.1 200 OK\r\na:b\r\n\r\nok"
           (str baos)))))

(deftest kondo
  (is (= []
         (:findings (kondo/run! {:lint ["src"]})))))

(deftest end-to-end
  (let [baos (ByteArrayOutputStream.)]
    (ds/process {::ds/handler (fn [req]
                                {:ring.response/body    "ok"
                                 :ring.response/headers {"car" "tar"}
                                 :ring.response/status  200})
                 ::ds/client  (reify ds/ISocket
                                (-input-stream [this]
                                  (str->is "GET /foo?query HTTP/1.1\r\nfoo:bar\r\n\r\ncar"))
                                (-output-stream [this] baos))})
    (is (= "HTTP/1.1 200 OK\r\ncar:tar\r\n\r\nok"
           (str baos)))))

(deftest request-helper
  (let [baos (ByteArrayOutputStream.)]
    (ds/process {::ds/handler (fn [req]
                                {:ring.response/body    "ok"
                                 :ring.response/headers {"car" "tar"}
                                 :ring.response/status  200})
                 ::ds/client  (reify ds/ISocket
                                (-input-stream [this]
                                  (request->input-stream
                                    {:ring.request/path    "/foo?query"
                                     :ring.request/method  :get
                                     :ring.request/headers {"foo" "bar"}
                                     :ring.request/body    "car"}))
                                (-output-stream [this] baos))})
    (is (= "HTTP/1.1 200 OK\r\ncar:tar\r\n\r\nok"
           (str baos)))))
