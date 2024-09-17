(ns zuzhi.chaptify.components.topics
  (:require
    [re-frame.core :as rf]
    [reagent.core :as r]
    [zuzhi.chaptify.db :refer [add-sub-topic add-topic delete-topic]]))


(defn handle-topic-submit
  [topic-name project-id user-id]
  (fn [event]
    (.preventDefault event)
    (add-topic @topic-name project-id user-id)
    (reset! topic-name "")))


(defn NewTopicForm
  [project-id]
  (let [topic-name (r/atom "")
        user (rf/subscribe [:user])
        user-id (:id @user)]
    (fn []
      [:form {:on-submit (handle-topic-submit topic-name project-id user-id)}
       [:input {:type "text"
                :value @topic-name
                :on-change #(reset! topic-name (-> % .-target .-value))}]
       [:button {:type "submit" :style {:padding-left 5}} "save"]])))


(defn handle-sub-topic-submit
  [project-id parent-id topic-name user-id]
  (fn [event]
    (.preventDefault event)
    (add-sub-topic project-id parent-id @topic-name user-id)
    (reset! topic-name "")))


(defn NewSubTopicForm
  [parent-id project-id]
  (let [topic-name (r/atom "")
        user (rf/subscribe [:user])
        user-id (:id @user)]
    (fn []
      [:form {:on-submit (handle-sub-topic-submit project-id parent-id topic-name user-id)}
       [:input {:type "text"
                :value @topic-name
                :on-change #(reset! topic-name (-> % .-target .-value))}]
       [:button {:type "submit" :style {:padding-left 5}} "save"]])))


(defn Topics
  [topics]
  [:ul
   (for [t topics]
     [:li {:key (:id t)} (:name t)
      [:button {:style {:padding-left 8}
                :on-click #(delete-topic (:id t))} "delete"]])])
