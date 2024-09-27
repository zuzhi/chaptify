(ns zuzhi.chaptify.events.projects
  (:require
    [clojure.string :as str :refer [join]]
    [zuzhi.chaptify.db :refer [add-project rename-project rename-topic]]
    [zuzhi.chaptify.util :refer [transform-project]]))


(defn build-sub-topics
  [topic level]
  (let [children (:children topic)
        list-items (for [child children]
                     (str "<li class=\"ql-indent-" level "\">" (:name child) "</li>" (build-sub-topics child (inc level))))
        list-items-str (join "" list-items)]
    list-items-str))


(defn build-topics
  [project]
  (let [transformed (transform-project project)
        topics (:topics transformed)
        sorted-topics (sort-by :createdAt < topics)
        list-items (for [topic sorted-topics]
                     (str "<li>" (:name topic) "</li>" (build-sub-topics topic 1)))
        list-items-str (join "" list-items)]
    (str "<ul>" list-items-str "</ul")))


(defn handle-open-in-editor
  [project editor-form-visibility-ref editor-ref]
  (when-let [set-visible (:set-visible @editor-form-visibility-ref)]
    (set-visible true))
  (when-let [set-value (:set-value @editor-ref)]
    (set-value (build-topics project)))
  (when-let [set-project (:set-project @editor-ref)]
    (set-project project)))


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
