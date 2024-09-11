(ns zuzhi.chaptify.components.topics
  (:require
    [reagent.core :as r]
    [zuzhi.chaptify.components.togglable :refer [Togglable]]
    [zuzhi.chaptify.db :refer [get-topics add-topic add-sub-topic delete-topic]]))


(defn handle-submit
  [project-id topic-name]
  (fn [event]
    (.preventDefault event)
    (add-topic project-id @topic-name)
    (reset! topic-name "")))


(defn NewTopicForm
  [project-id]
  (r/with-let [topic-name (r/atom "")]
              [:form {:on-submit (handle-submit project-id topic-name)}
               [:input {:type "text"
                        :value @topic-name
                        :on-change #(reset! topic-name (-> % .-target .-value))}]
               [:button {:type "submit" :style {:padding-left 5}} "save"]]))


(defn handle-sub-topic-submit
  [project-id parent-id topic-name]
  (fn [event]
    (.preventDefault event)
    (add-sub-topic project-id parent-id @topic-name)
    (reset! topic-name "")))


(defn NewSubTopicForm
  [parent-id project-id]
  (r/with-let [topic-name (r/atom "")]
              [:form {:on-submit (handle-sub-topic-submit project-id parent-id topic-name)}
               [:input {:type "text"
                        :value @topic-name
                        :on-change #(reset! topic-name (-> % .-target .-value))}]
               [:button {:type "submit" :style {:padding-left 5}} "save"]]))


(defn Topics
  [topics]
  [:ul
   (for [t topics]
     [:li {:key (:id t)} (:name t)
      [:button {:style {:padding-left 8}
                :on-click #(delete-topic (:id t))} "delete"]])])


(defn TopicsPage
  []
  (let [result (get-topics)
        {:keys [isLoading error data]} (js->clj result :keywordize-keys true)
        topics (or (:topics data) [])

        visibility-ref (r/atom nil)]

    (cond
      isLoading
      [:div "loading"]

      error
      [:div (str "error fetching data: " (.-message error))]

      :else
      [:div
       [Togglable
        {:buttonLabel "new topic"
         :ref visibility-ref}
        ^{:key "new-topic-form"} [NewTopicForm]]
       [Topics topics]])))
