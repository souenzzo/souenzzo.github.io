(ns sample.app
  (:require [goog.dom :as gdom]
            ["react" :as r]
            ["react-dom" :as rd]
            [br.com.souenzzo.use-eql :as use-eql]))

(defn Counter
  []
  (let [conn (use-eql/fetch {::use-eql/query [::current-count]})]
    (r/createElement
      "div"
      #js{}
      (r/createElement
        "pre"
        #js{}
        (pr-str @conn))
      (r/createElement
        "button"
        #js{:onClick (fn []
                       (use-eql/transact conn `[(increment {})]))}
        "+"))))

(defn TodoApp
  []
  (let [conn (use-eql/fetch {::use-eql/query [{:app.todo/todos
                                               [:app.todo/id
                                                :app.todo/text
                                                :app.todo/done?]}]})
        {:app.todo/keys [todos]
         :as            tree} @conn
        [text set-text] (r/useState "")]
    (r/createElement
      "div"
      #js{}
      (r/createElement
        "ul"
        #js{}
        (for [{:app.todo/keys [id text]} todos]
          (r/createElement
            "li"
            #js{:key id}
            text
            (r/createElement
              "button"
              #js{:onClick (fn []
                             (use-eql/transact conn `[(app.todo/remove ~{:app.todo/id id})]))}
              "x"))))

      (r/createElement
        "form"
        #js{:onSubmit (fn [e]
                        (.preventDefault e)
                        (use-eql/transact conn `[(app.todo/new-todo ~{:app.todo/text text})])
                        (set-text ""))}
        (r/createElement
          "input"
          #js{:value text
              :onChange (fn [e]
                          (set-text (-> e .-target .-value)))})))))

(defn Root
  []
  (let [[show? set-show?] (r/useState false)]
    (r/createElement
      "div"
      #js{}
      (r/createElement
        "button"
        #js{:onClick (fn []
                       (set-show? (not show?)))}
        (pr-str "toggle" [show?]))
      (when show?
        (r/createElement Counter))
      (when show?
        (r/createElement TodoApp)))))

(defn ^:export start
  [target]
  (rd/render
    (r/createElement Root #js {})
    (gdom/getElement target)))

(defn after-load
  [])
