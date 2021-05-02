(ns sample.app
  (:require [goog.dom :as gdom]
            ["react" :as r]
            ["react-dom" :as rd]
            [br.com.souenzzo.use-eql :as use-eql]))

(defn WithData
  []
  (let [[value dispatch] (use-eql/fetch {::use-eql/query [::hello]})]
    (r/createElement
      "div"
      #js{}
      (r/createElement
        "pre"
        #js{}
        (pr-str value))
      (r/createElement
        "button"
        #js{:onClick (fn []
                       (dispatch `[(add {::v 2})]))}
        "+2"))))

(defn Hello
  []
  (let [[show? set-show?] (r/useState false)]
    (r/createElement
      "div"
      #js{}
      (when show?
        (r/createElement WithData))
      (r/createElement
        "button"
        #js{:onClick (fn []
                       (set-show? (not show?)))}
        (pr-str "toggle" [show?])))))

(defn ^:export start
  [target]
  (rd/render
    (r/createElement Hello #js {})
    (gdom/getElement target)))

(defn after-load
  [])
