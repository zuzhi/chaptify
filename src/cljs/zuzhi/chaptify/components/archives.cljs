(ns zuzhi.chaptify.components.archives
  (:require
    [clojure.string :as str]
    [zuzhi.chaptify.db :refer [delete-project get-projects unarchive-project]]
    [zuzhi.chaptify.util :refer [transform-project]]))


(defn TopicLine
  [{:keys [id name status children]} project-id]
  [:li {:key id}
   [:span {:class (str/replace status " " "-")} name]
   [:ul
    (for [t children]
      ^{:key (:id t)} [TopicLine t project-id])]])


(defn ProjectLine
  [{:keys [id name topics] :as project}]
  (let [finished-topics (filter #(or (= (:status %) "done")
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
                 :on-click #(delete-project project)} "delete"]
       [:button {:style {:padding-left 8}
                 :on-click #(unarchive-project (:id project))} "unarchive"]
       [:ul
        (for [t direct-topics]
          ^{:key (:id t)} [TopicLine t id])]])))


(defn ArchivesPage
  []
  (let [result (get-projects "archived" true)
        {:keys [isLoading error data]} (js->clj result :keywordize-keys true)
        projects (or (:projects data) [])]
    (cond
      isLoading
      [:div "loading"]

      error
      [:div (str "error fetching data: " (.-message error))]

      :else
      [:ul
       (for [p projects]
         ^{:key (:id p)} [ProjectLine p])])))
