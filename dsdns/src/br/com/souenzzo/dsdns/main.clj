(ns br.com.souenzzo.dsdns.main
  (:require [clojure.pprint :as pp])
  (:import (java.net DatagramPacket DatagramSocket InetAddress)
           (java.nio.charset StandardCharsets)))

(set! *print-namespace-maps* false)
(set! *warn-on-reflection* true)

(defn create-server
  [{::keys [port max-size]}]
  (with-open [socket (DatagramSocket. (int port))]
    (let [buff (byte-array max-size)
          packet (DatagramPacket. buff (alength buff))
          _ (.receive socket packet)
          length (.getLength packet)
          offset (.getOffset packet)]
      {::size           (- length offset)
       ::address        (.getAddress packet)
       ::socket-address (.getSocketAddress packet)
       ::port           (.getPort packet)
       ::msg            (String. (.getData packet)
                                 offset length
                                 StandardCharsets/UTF_8)})))

(comment
  ;; cliente
  (future (pp/pprint (create-server {::port 8888 ::max-size 1e3})))
  (with-open [client (DatagramSocket.)]
    (let [target (InetAddress/getByName "127.0.0.1")
          data (.getBytes "abc")
          packet (DatagramPacket. data 0 (alength data) target 8888)]
      (.send client packet))))


(defn -main
  [& _]
  (prn :ok))
