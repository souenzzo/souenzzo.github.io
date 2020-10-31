(ns sample.app
  (:require [goog.dom :as gdom]
            [reagent.dom :as rd]
            [re-frame.core :as rf]
            [com.wsscode.pathom.connect :as pc]
            [br.com.souenzzo.redbeql :as redbeql]
            [clojure.pprint :as pp]))

;; ui

(defn current-state
  []
  [:h1
   (pr-str @(rf/subscribe [:counter]))])

(defn controls
  []
  [:<>
   [:button {:on-click #(rf/dispatch [:dec])}
    "-"]
   [:button {:on-click #(rf/dispatch [:inc])}
    "+"]])

(defn ui
  []
  [:<>
   [:main
    [current-state]
    [controls]]
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
                  {::counter @counter}))])

;; Events

(rf/reg-event-db :initialize (fn [_ _]
                               {::counter 0}))
(rf/reg-event-fx :inc
                 (fn [_ _]
                   {:eql `[{(increment {})
                            [::counter]}]}))
(rf/reg-event-fx :dec
                 (fn [_ _]
                   {:eql `[{(decrement {})
                            [::counter]}]}))


;; Subs

(rf/reg-sub :counter (fn [db _] (get-in db [::counter])))


(defonce app-state (redbeql/eql {::pc/indexes        (pc/register {} register)
                                 ::redbeql/on-result ::on-result
                                 ::counter           (atom 3)}))

(rf/reg-fx :eql app-state)
(rf/reg-event-db ::on-result redbeql/on-result)


(defn ^:export start
  [target]
  (let [el (gdom/getElement target)]
    (rf/dispatch-sync [:initialize])
    (rd/render [ui] el)))
