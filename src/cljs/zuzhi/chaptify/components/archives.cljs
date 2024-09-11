(ns zuzhi.chaptify.components.archives
  (:require
    [zuzhi.chaptify.db :refer [delete-project get-projects unarchive-project]]))


(defn ArchivesPage
  []
  (let [result (get-projects "archived" true)
        {:keys [isLoading error data]} (js->clj result :keywordize-keys true)
        projects (or (:projects data) [])]
    (js/console.log projects)
    (cond
      isLoading
      [:div "loading"]

      error
      [:div (str "error fetching data: " (.-message error))]

      :else
      [:ul
       (for [p projects] [:li {:key (:id p)} (:name p)
                          [:button {:style {:padding-left 8}
                                    :on-click #(delete-project (:id p) (:topics p))} "delete"]
                          [:button {:style {:padding-left 8}
                                    :on-click #(unarchive-project (:id p))} "unarchive"]])])))
