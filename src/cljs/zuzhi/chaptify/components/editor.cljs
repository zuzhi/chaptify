(ns zuzhi.chaptify.components.editor
  (:require
    ["react-quill" :as ReactQuill]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [zuzhi.chaptify.events.editor :refer [handle-editor-save]]))


(defn indent-level
  [li]
  (let [class-list (.-classList li)
        indent-class (first class-list)]
    (if indent-class
      (let [matches (re-find #"\d+" indent-class)]
        (when matches
          (js/parseInt matches)))
      0)))


(defn parse-topics
  [content]
  (let [temp-div (.createElement js/document "div")]
    (set! (.-innerHTML temp-div) content)
    (let [topics (map (fn [li]
                        {:name (.-textContent li)
                         :indent (indent-level li)})
                      (array-seq (.querySelectorAll temp-div "li")))]
      topics)))


(defn Editor
  [{:keys [ref]}]
  (let [value (r/atom "")
        set-value #(reset! value %)
        project (r/atom nil)
        set-project #(reset! project %)

        user (rf/subscribe [:user])
        user-id (:id @user)]

    (when ref
      (reset! ref {:set-value set-value
                   :set-project set-project}))

    (fn []
      [:form {:on-submit (fn [e]
                           (.preventDefault e)
                           (let [topics (parse-topics @value)]
                             (when (js/window.confirm (str "(re)initialize " (:name @project) "? status will be lost."))
                               (handle-editor-save project topics user-id))))}
       [:> ReactQuill
        {:theme "snow"
         :modules {:toolbar [{:list "bullet"}]}
         :formats ["list" "indent"]
         :value @value
         :on-change set-value}]
       [:button {:type "submit"} "save"]])))
