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


(deftest true-and-false
  (fact
    (dvm/render-to-string {}
                          [:p {:a true
                               :b false
                               :c nil
                               :d ""
                               :e 0}])
    => "<p a d=\"\" e=\"0\"></p>"))


(deftest escape
  (fact
    (dvm/render-to-string {} [:p "<"])
    => "<p>&lt;</p>"))

(comment
  (dvm/render-to-string {} [:>])
  => "Uncaught DOMException: String contains an invalid character"
  (dvm/render-to-string {} [:a {:href (with-meta {:route-name ...}
                                                 ...)}])
  => [:a {:href (route/url-for ...)}]
  (import '(nu.validator.validation SimpleDocumentValidator)
          '(org.xml.sax ErrorHandler InputSource)
          '(java.io StringReader))
  (defn validation-errors
    "
    https://github.com/Be-Nice-Now/patterns/blob/51f6bdb2f0bdd260c5a7b3fcb3494fd4ed5af130/test/patterns/test_utils.clj
    "
    [html]
    (let [errs (atom [])
          error-handler (reify ErrorHandler
                          (warning [_this ex]
                            (swap! errs conj {:level :warning
                                              :ex    ex}))
                          (error [_this ex]
                            (swap! errs conj {:level :error
                                              :ex    ex}))
                          (fatalError [_this ex]
                            (swap! errs conj {:level :fatal
                                              :ex    ex})))]
      (doto (SimpleDocumentValidator.)
        (.setUpMainSchema "http://s.validator.nu/html5-rdfalite.rnc" error-handler)
        (.setUpValidatorAndParsers error-handler false false)
        (.checkHtmlInputSource (-> html StringReader. InputSource.)))
      @errs)))
