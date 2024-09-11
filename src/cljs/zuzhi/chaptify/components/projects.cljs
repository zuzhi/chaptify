(ns zuzhi.chaptify.components.projects
  (:require
    [clojure.string :as str]
    [reagent.core :as r]
    [zuzhi.chaptify.components.togglable :refer [Togglable]]
    [zuzhi.chaptify.components.topics :refer [NewSubTopicForm NewTopicForm]]
    [zuzhi.chaptify.db :refer [add-project archive-project delete-project
                               delete-topic get-projects rename-project
                               update-topic-status]]))


(defn handle-submit
  [value]
  (fn [event]
    (.preventDefault event)
    (add-project @value)
    (reset! value "")))


(defn handle-edit-submit
  [id name]
  (fn [event]
    (.preventDefault event)
    (rename-project id @name)
    (reset! name "")))


(defn NewProjectForm
  []
  (r/with-let [project-name (r/atom "")]
              [:form {:on-submit (handle-submit project-name)}
               [:input {:type "text"
                        :value @project-name
                        :on-change #(reset! project-name (-> % .-target .-value))}]
               [:button {:type "submit" :style {:padding-left 5}} "save"]]))


(defn EditProjectForm
  [project-id name]
  (r/with-let [project-name (r/atom name)]
              [:form {:on-submit (handle-edit-submit project-id project-name)}
               [:input {:type "text"
                        :value @project-name
                        :on-change #(reset! project-name (-> % .-target .-value))}]
               [:button {:type "submit" :style {:padding-left 5}} "save"]]))


(defn TopicLine
  [{:keys [id name status children] :as parent} project-id]
  (let [visibility-ref (r/atom nil)]
    [:li {:key id}
     [:span {:class (str/replace status " " "-")} name]
     [:button {:style {:padding-left 8}
               :on-click #(delete-topic id children)} "delete"]
     [:button {:style {:padding-left 8}
               :on-click #(update-topic-status id "pending" project-id)} "pending"]
     [:button {:style {:padding-left 8}
               :on-click #(update-topic-status id "in progress" project-id)} "in progress"]
     [:button {:style {:padding-left 8}
               :on-click #(update-topic-status id "done" project-id)} "done"]
     [:button {:style {:padding-left 8}
               :on-click #(update-topic-status id "skip" project-id)} "skip"]
     [:button {:style {:padding-left 8}
               :on-click #(update-topic-status id "skim" project-id)} "skim"]
     [Togglable
      {:buttonLabel "new topic"
       :ref visibility-ref}
      ^{:key "new-sub-topic-form"} [NewSubTopicForm id project-id]]
     [:ul
      (for [t children]
        ^{:key (:id t)} [TopicLine t project-id])]]))


(defn ProjectLine
  [{:keys [id name progress topics] :as project}]
  (let [visibility-ref (r/atom nil)]
    [:li {:key id}
     [:span.project (str name " " progress "%")]
     ;; [Togglable
     ;; {:buttonLabel "rename"
     ;;  :ref visibility-ref}
     ;; ^{:key "edit-project-form"} [EditProjectForm id name]]
     [:button {:style {:padding-left 8}
               :on-click #(delete-project id topics)} "delete"]
     [:button {:style {:padding-left 8}
               :on-click #(archive-project id)} "archive"]
     [Togglable
      {:buttonLabel "new topic"
       :ref visibility-ref}
      ^{:key "new-topic-form"} [NewTopicForm id]]
     [:ul
      (for [t topics]
        ^{:key (:id t)} [TopicLine t id])]]))


(defn has-matching-parent?
  [topic-id sub-topic]
  (some #(= (:id %) topic-id) (:parent sub-topic)))


(defn transform-topic
  [topic sub-topics]
  (let [children (filter #(has-matching-parent? (:id topic) %) sub-topics)]
    (assoc topic :children (for [t children]
                             (transform-topic t sub-topics)))))


(defn transform-project
  [{:keys [id name status progress created_at topics]}]
  (let [direct-topics (filter #(= (:parent %) []) topics)
        sub-topics (filter #(not= (:parent %) []) topics)]
    {:id id
     :name name
     :status status
     :progress progress
     :created_at created_at
     :topics (for [t direct-topics]
               (transform-topic t sub-topics))}))


(defn transform-projects
  [projects]
  (for [p projects]
    (transform-project p)))


(defn Projects
  [projects]
  (let [transformed (transform-projects projects)]
    [:ul
     (for [p transformed]
       ^{:key (:id p)} [ProjectLine p])]))


(defn ProjectsPage
  []
  (let [result (get-projects "normal" true)
        {:keys [isLoading error data]} (js->clj result :keywordize-keys true)
        projects (or (:projects data) [])

        visibility-ref (r/atom nil)]

    (cond
      isLoading
      [:div "loading"]

      error
      [:div (str "error fetching data: " (.-message error))]

      :else
      [:div {:style {:margin-top 16}}
       [Togglable
        {:buttonLabel "new project"
         :ref visibility-ref}
        ^{:key "new-project-form"} [NewProjectForm]]
       [Projects projects]])))
