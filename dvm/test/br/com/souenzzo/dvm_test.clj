(ns br.com.souenzzo.dvm-test
  (:require [clojure.test :refer [deftest testing is]]
            [br.com.souenzzo.dvm :as dvm]
            [midje.sweet :refer [fact =>]]))

(defn ui-sum
  [{:keys [a]} {:keys [b]}]
  (if b
    [:ul
     [:li [:p (str (+ a b))]]
     [:li [ui-sum {}]]]
    [:p a]))

(deftest hello-world
  (fact
    (dvm/render-to-string {} [:div "Hello World!"])
    => "<div>Hello World!</div>")
  (fact
    (dvm/render-to-string {} [:div [:div "Hello"]])
    => "<div><div>Hello</div></div>")
  (fact
    (dvm/render-to-string {} [:div {:id "42" :value 42} "Hello"])
    => "<div id=\"42\" value=42>Hello</div>")
  (fact
    (dvm/render-to-string {:a 1}
                          [:div [ui-sum {:b 2}]])
    => "<div><ul><li><p>3</p></li><li><p>1</p></li></ul></div>")
  (fact
    (dvm/render-to-string {}
                          [:div
                           [:p {} "oi"]
                           (for [i (range 2)]
                             [:p (str "oi" i)])])
    => "<div><p>oi</p><p>oi0</p><p>oi1</p></div>")
  (fact
    (dvm/render-to-string {}
                          [:meta {:charset "utf-8"}])
    => "<meta charset=\"utf-8\">"))

(comment
  (dvm/render-to-string {} [:>])
  => "Uncaught DOMException: String contains an invalid character"
  (dvm/render-to-string {} [:a {:href (with-meta {:route-name ...}
                                                 ...)}])
  => [:a {:href (route/url-for ...)}]
  (let
    [baos (java.io.ByteArrayOutputStream.)
     source-code (nu.validator.source.SourceCode.)
     image-collector (nu.validator.servlet.imagereview.ImageCollector. source-code)
     emitter (nu.validator.messages.TextMessageEmitter. baos false)
     in (clojure.java.io/input-stream (.getBytes "<!DOCTYPE html><html><div></div></html>"))
     error-handler (nu.validator.messages.MessageEmitterAdapter. #"." source-code false image-collector 0 false emitter)
     validator (doto
                 (nu.validator.validation.SimpleDocumentValidator.)
                 (.setUpMainSchema "http://s.validator.nu/html5-rdfalite.rnc" (nu.validator.xml.SystemErrErrorHandler.))
                 (.setUpValidatorAndParsers error-handler true false)
                 (.checkHtmlInputSource (org.xml.sax.InputSource. in)))]
    [(.getErrors error-handler) (str baos)]))
