(ns sample.app
  (:require [goog.dom :as gdom]
            [reagent.dom :as rd]
            ["react" :as r]
            [br.com.souenzzo.use-eql :as use-eql]
            [br.com.souenzzo.use-eql.fetch :as use-eql.fetch]))

(defn Counter
  []
  (let [conn (use-eql/impl {::use-eql/query [::current-count]})]
    [:div
     (if (use-eql/loading? conn)
       "loading ..."
       "ok ...")
     [:pre (pr-str @conn)]
     [:button
      {:on-click (fn []
                   (use-eql/transact conn `[(increment {})]))}
      "+"]]))

(defn TodoApp
  []
  (let [conn (use-eql/impl {::use-eql/query [{:app.todo/todos
                                              [:app.todo/id
                                               :app.todo/text
                                               :app.todo/done?]}]})
        {:app.todo/keys [todos]
         :as            tree} @conn
        [text set-text] (r/useState "")]
    [:div
     (if (use-eql/loading? conn)
       "loading ..."
       "ok ...")
     [:ul
      (for [{:app.todo/keys [id text]} todos]
        [:li {:key id}
         text
         [:button
          {:on-click (fn []
                       (use-eql/transact conn `[(app.todo/remove ~{:app.todo/id id})]))}
          "x"]])]
     [:form
      {:on-submit (fn [e]
                    (.preventDefault e)
                    (use-eql/transact conn `[(app.todo/new-todo ~{:app.todo/text text})])
                    (set-text ""))}
      [:input {:value     text
               :on-change (fn [e]
                            (set-text (-> e .-target .-value)))}]]]))


(defn Root
  []
  (let [[show? set-show?] (r/useState false)]
    [:div
     [:> (.-Provider use-eql/driver)
      {:value use-eql.fetch/driver}
      [:button
       {:onClick (fn []
                   (set-show? (not show?)))}
       (pr-str "toggle" [show?])]
      (when show?
        [:<>
         [:f> Counter]
         [:f> TodoApp]])]]))

(defn ^:export start
  [target]
  (rd/render [:f> Root] (gdom/getElement target)))

(defn after-load
  [])
