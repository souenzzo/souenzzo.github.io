(ns sample.app
  (:require [goog.dom :as gdom]
            [reagent.dom :as rd]
            [re-frame.core :as rf]
            [com.wsscode.pathom.connect :as pc]
            [br.com.souenzzo.redbeql :as redbeql]
            [clojure.pprint :as pp]))

;; ui
(defn ui
  []
  [:<>
   [:main
    [:h1
     (pr-str @(rf/subscribe [:counter]))]
    [:button {:on-click #(rf/dispatch [:dec])}
     "-"]
    [:button {:on-click #(rf/dispatch [:inc])}
     "+"]
    [:div
     [:form
      {:onSubmit (fn [e]
                   (let [txt (-> e .-target .-elements first .-value)]
                     (.preventDefault e)
                     (rf/dispatch [:new-todo txt])))}
      [:input]]
     [:ul
      (for [{:keys [text]} @(rf/subscribe [:todos])]
        [:li {:key text}
         text])]]]
   [:footer
    [:pre (with-out-str (pp/pprint @re-frame.db/app-db))]]])

;; app


(def register
  [(pc/mutation `increment
                {}
                (fn [{::keys [counter]} {}]
                  {::counter (swap! counter inc)}))
   (pc/mutation `decrement
                {}
                (fn [{::keys [counter]} {}]
                  {::counter (swap! counter dec)}))
   (pc/resolver `counter
                {::pc/output [::counter]}
                (fn [{::keys [counter]} {}]
                  {::counter @counter}))
   (pc/mutation `new-todo
                {::pc/output []}
                (fn [{::keys [todos]} {:keys [text]}]
                  (let [todos (swap! todos conj text)]
                    {::todos (for [todo todos]
                               {:text todo})})))])

;; Events

(rf/reg-event-fx :inc
                 (fn [_ _]
                   {:eql `[{(increment {})
                            [::counter]}]}))


(rf/reg-event-fx :dec
                 (fn [_ _]
                   {:eql `[{(decrement {})
                            [::counter]}]}))

(rf/reg-event-fx :new-todo
                 (fn [_ [_ text]]
                   {:eql `[{(new-todo ~{:text text})
                            [^{:index :text}
                             {::todos [:text]}]}]}))


;; Subs

(rf/reg-sub :counter (fn [{::keys [counter]} _] counter))
(rf/reg-sub :todos (fn [{::keys [todos]
                         :as    db} _]
                     (for [todo todos]
                       (get-in db todo))))


(defonce app-state (redbeql/eql {::pc/indexes        (pc/register {} register)
                                 ::redbeql/on-result ::on-result
                                 ::todos             (atom #{"a1" "a2"})
                                 ::counter           (atom 3)}))

(rf/reg-fx :eql app-state)
(rf/reg-event-db ::on-result redbeql/on-result)


(defn ^:export start
  [target]
  (let [el (gdom/getElement target)]
    ;; (rf/dispatch-sync [:initialize])
    (rd/render [ui] el)))
