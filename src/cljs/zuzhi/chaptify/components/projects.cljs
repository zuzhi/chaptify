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
  [{:keys [id name] :as project} input-ref]
  (r/with-let [project-name (r/atom name)]
              [:form {:on-submit (handle-edit-submit id project-name)}
               [:input {:type "text"
                        :value @project-name
                        :ref #(reset! input-ref %)
                        :on-change #(reset! project-name (-> % .-target .-value))}]
               [:button {:type "submit" :style {:padding-left 8}} "save"]]))


(defn TopicLine
  [{:keys [id name status children] :as parent} project-id]
  (let [visibility-ref (r/atom nil)]
    [:li {:key id}
     [:span {:class (str/replace status " " "-")} name]
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
     [Togglable
      {:buttonLabel "new topic"
       :ref visibility-ref}
      ^{:key "new-sub-topic-form"} [NewSubTopicForm id project-id]]
     [:ul
      (for [t children]
        ^{:key (:id t)} [TopicLine t project-id])]]))


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
        ^{:key "edit-project-form"} [EditProjectForm project input-ref]]
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
