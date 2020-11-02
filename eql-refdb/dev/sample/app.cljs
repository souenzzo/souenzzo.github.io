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
  (let [{::keys [counter todos]} @(rf/subscribe [:select [::counter
                                                          {::todos [:text]}]])]
    [:<>
     [:main
      [:h1
       (pr-str counter)]
      [:button {:on-click #(rf/dispatch [:tx! `[{(decrement {})
                                                 [::counter]}]])}
       "-"]
      [:button {:on-click #(rf/dispatch [:tx! `[{(increment {})
                                                 [::counter]}]])}
       "+"]
      [:div
       [:form
        {:onSubmit (fn [e]
                     (let [txt (-> e .-target .-elements first .-value)]
                       (.preventDefault e)
                       (rf/dispatch [:tx! `[{(new-todo ~{:text txt})
                                             [^{:index :text}
                                              {::todos [:text]}]}]])))}
        [:input]]
       [:ul
        (for [{:keys [text]} todos]
          [:li {:key text}
           text])]]]
     [:footer
      [:pre (with-out-str (pp/pprint @re-frame.db/app-db))]]]))

;; register

(pc/defmutation increment [{::keys [counter]} _params]
  {}
  {::counter (swap! counter inc)})

(pc/defmutation decrement [{::keys [counter]} _params]
  {}
  {::counter (swap! counter inc)})

(pc/defmutation new-todo [{::keys [todos]} {:keys [text]}]
  {::pc/params [:text]}
  (let [todos (swap! todos conj text)]
    {::todos (for [todo todos]
               {:text todo})}))


(pc/defresolver counter [{::keys [counter]} _input]
  {::counter @counter})

(pc/defresolver todos [{::keys [todos]} _input]
  {::todos (for [todo @todos]
             {:text todo})})

;; Connect pathom with re-frame

(defonce app-state
         {::pc/indexes        (pc/register {} [increment decrement counter todos new-todo])
          ::redbeql/on-result ::on-result
          ::redbeql/parser-fx ::parser
          ::todos             (atom #{"a1" "a2"})
          ::counter           (atom 3)})

;; should be ::redbeql/parser-fx
(rf/reg-fx ::parser (redbeql/parser-fx app-state))
;; should be ::redbeql/on-result
(rf/reg-event-db ::on-result redbeql/on-result)

;; will be used to send queries/mutations to remote
(rf/reg-event-fx :tx! (redbeql/parser-event-fx app-state))
;; will be used to select normalized data from current state
(rf/reg-sub :select redbeql/select-sub)


(defn ^:export start
  [target]
  (let [el (gdom/getElement target)]
    (rf/dispatch [:tx! `[::counter
                         ^{:index :text}
                         {::todos [:text]}]])
    (rd/render [ui] el)))
