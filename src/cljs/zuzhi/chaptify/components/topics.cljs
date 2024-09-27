(ns zuzhi.chaptify.components.topics
  (:require
    [re-frame.core :as rf]
    [reagent.core :as r]
    [zuzhi.chaptify.events.topics :refer [handle-sub-topic-submit
                                          handle-topic-submit]]))


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
