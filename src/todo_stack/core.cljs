(ns todo-stack.core
  (:require [cljs.reader :as reader]
            [reagent.core :as reagent]))

;; TODO: Enable better persistence
;; TODO: Allow for multiple "contexts" in which cards may live (e.g. "Home" and "Work")
;; TODO: Style

(enable-console-print!)


(defonce app-state
  (reagent/atom
   (if-let [init-state (.getItem js/localStorage "app-state")]
     (reader/read-string init-state)
     {:title "Card Stack"
      :complete-cards []
      :cards [{:title "Add your first card"
               :description "Click the \"Add Card\" button below to get started"
               :editing? false}]})))

;; Helpers
(defn indexed-seq [xs]
  (map vector (range) xs))

(def empty-card
  {:title ""
   :description ""
   :editing? true})

;; Actions
(defn push-card! [card]
  (swap! app-state update-in [:cards] conj card))

(defn pop-card! []
  (swap! app-state
         (fn [state]
           (-> state
               (update-in [:complete-cards] conj (-> state :cards last))
               (update-in [:cards] #(into [] (butlast %)))))))

(defn update-top-card-prop [prop]
  (fn [val]
    (swap! app-state
           update-in [:cards]
           (fn [cards]
             (conj
              (into [] (butlast cards))
              (assoc (last cards) prop val))))))

(defn save-state! []
  (.setItem js/localStorage "app-state" (pr-str @app-state)))


;; Components
(defn content [& children]
  [:div.content-wrap
   [:div.content
    (for [[idx child] (indexed-seq children)]
      [:div {:key idx} child])]])


(defn header []
  [:div.header
   [:div.card-count {:title (str "Completed: " (count (:complete-cards @app-state)))}
    [:span (count (:cards @app-state))]]
   [content
    [:h2.title (:title @app-state)]]])

(defn footer []
  [:div.footer
   [content
    [:button {:on-click save-state!} "Save"]]])

(defn edit-title [title on-change]
  [:div.input-group
   [:div.field-label
    [:label {:for "card-title-input"} "Title"]]
   [:input {:id "card-title-input"
            :type "text"
            :value title
            :on-change #(on-change (.. % -target -value))}]])

(defn edit-description [description on-change]
  [:div.input-group
   [:div.field-label
    [:label {:for "card-description-input"} "Description"]]
   [:textarea {:id "card-description-input"
               :value description
               :on-change #(on-change (.. % -target -value))}]])


(defn render-card [{:keys [title description editing?]}]
  [:div.card
   [:div.header
    (if editing?
      [edit-title title (update-top-card-prop :title)]
      [:h4.title title])
    [:a.remove {:style {:cursor "pointer"}
                :on-click pop-card!} "X"]]
   [:div.body
    (if editing?
      [edit-description description (update-top-card-prop :description)]
      [:p.description description])]])

(defn cards []
  [content
   [:div.cards
    (if-let [top-card (last (:cards @app-state))]
      [render-card top-card]
      [:p.info "No Cards Left!"])]])

(defn add-card []
  (fn []
    [content
     [:div.add-card
      [:button {:on-click #(do (push-card! empty-card))}
       "Add"]]]))

(defn app []
  [:div.app
   [header]
   [cards]
   [add-card]
   [footer]])


(reagent/render
 [app]
 (.getElementById js/document "app"))
