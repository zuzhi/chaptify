(ns zuzhi.chaptify.components.projects
  (:require
    [clojure.string :as str]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [zuzhi.chaptify.components.togglable :refer [Togglable]]
    [zuzhi.chaptify.components.topics :refer [NewSubTopicForm NewTopicForm]]
    [zuzhi.chaptify.db :refer [add-project archive-project delete-project
                               delete-topic get-projects rename-project rename-topic
                               update-topic-status]]
    [zuzhi.chaptify.util :refer [transform-project]]))


(defn handle-project-submit
  [name user-id]
  (fn [event]
    (.preventDefault event)
    (add-project @name user-id)
    (reset! name "")))


(defn handle-edit-submit
  [id name]
  (fn [event]
    (.preventDefault event)
    (rename-project id @name)
    (reset! name "")))


(defn handle-edit-topic-submit
  [id name]
  (fn [event]
    (.preventDefault event)
    (rename-topic id @name)
    (reset! name "")))


(defn NewProjectForm
  []
  (let [project-name (r/atom "")
        user (rf/subscribe [:user])
        user-id (:id @user)]
    (fn []
      ;; (rf/dispatch [:set-projects projects])
      [:form {:on-submit (handle-project-submit project-name user-id)}
       [:input {:type "text"
                :value @project-name
                :on-change #(reset! project-name (-> % .-target .-value))}]
       [:button {:type "submit" :style {:padding-left 5}} "save"]])))


(defn EditProjectForm
  [{:keys [id name]} input-ref]
  (let [project-name (r/atom name)]
    (fn []
      [:form {:on-submit (handle-edit-submit id project-name)}
       [:input {:type "text"
                :value @project-name
                :ref #(reset! input-ref %)
                :on-change #(reset! project-name (-> % .-target .-value))}]
       [:button {:type "submit" :style {:padding-left 8}} "save"]])))


(defn EditTopicForm
  [{:keys [id name]} input-ref]
  (let [topic-name (r/atom name)]
    (fn []
      [:form {:on-submit (handle-edit-topic-submit id topic-name)}
       [:input {:type "text"
                :value @topic-name
                :ref #(reset! input-ref %)
                :on-change #(reset! topic-name (-> % .-target .-value))}]
       [:button {:type "submit" :style {:padding-left 8}} "save"]])))


(defn TopicLine
  [{:keys [id name status children] :as topic} project-id]
  (let [edit-topic-form-visibility-ref (r/atom nil)
        input-ref (r/atom nil)
        visibility-ref (r/atom nil)]
    [:li {:key id}
     [:span {:class (str/replace status " " "-")} name]
     [:button {:style {:padding-left 8}
               :on-click #(when-let [set-visible (:set-visible @edit-topic-form-visibility-ref)]
                            (set-visible true))} "rename"]
     [:button {:style {:padding-left 8}
               :on-click #(delete-topic id children)} "delete"]
     [:button {:style {:padding-left 8}
               :on-click #(update-topic-status id "pending")} "pending"]
     [:button {:style {:padding-left 8}
               :on-click #(update-topic-status id "in progress")} "in progress"]
     [:button {:style {:padding-left 8}
               :on-click #(update-topic-status id "done")} "done"]
     [:button {:style {:padding-left 8}
               :on-click #(update-topic-status id "skip")} "skip"]
     [:button {:style {:padding-left 8}
               :on-click #(update-topic-status id "skim")} "skim"]
     [Togglable {:ref edit-topic-form-visibility-ref
                 :on-show #(when @input-ref (.focus @input-ref))}
      ^{:key "edit-project-form"}
      [EditTopicForm topic input-ref]]
     [Togglable
      {:buttonLabel "new topic"
       :ref visibility-ref}
      ^{:key "new-sub-topic-form"} [NewSubTopicForm id project-id]]
     [:ul
      (for [t children]
        ^{:key (:id t)} [TopicLine t project-id])]]))


(defn ProjectLine
  [{:keys [id name topics] :as project}]
  (let [edit-form-visibility-ref (r/atom nil)
        input-ref (r/atom nil)
        visibility-ref (r/atom nil)
        finished-topics (filter #(or (= (:status %) "done")
                                     (= (:status %) "skip")
                                     (= (:status %) "skim"))
                                topics)
        progress (int (* (/ (count finished-topics) (count topics)) 100))
        transformed (transform-project project)
        direct-topics (:topics transformed)]
    (fn []
      [:li {:key id}
       [:span.project (str name " " progress "%")]
       [:button {:style {:padding-left 8}
                 :on-click #(when-let [set-visible (:set-visible @edit-form-visibility-ref)]
                              (set-visible true))} "rename"]
       [:button {:style {:padding-left 8}
                 :on-click #(delete-project project)} "delete"]
       [:button {:style {:padding-left 8}
                 :on-click #(archive-project id)} "archive"]
       [Togglable {:ref edit-form-visibility-ref
                   :on-show #(when @input-ref (.focus @input-ref))}
        ^{:key "edit-project-form"}
        [EditProjectForm project input-ref]]
       [Togglable {:buttonLabel "new topic"
                   :ref visibility-ref}
        ^{:key "new-topic-form"} [NewTopicForm id]]
       [:ul
        (for [t direct-topics]
          ^{:key (:id t)} [TopicLine t id])]])))


(defn Projects
  [projects]
  [:ul
   (for [p projects]
     (let [project-line (fn [] [ProjectLine p])]
       ^{:key (:id p)} [project-line]))])


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
