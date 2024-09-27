(ns zuzhi.chaptify.events.topics
  (:require
    [zuzhi.chaptify.db :refer [add-sub-topic add-topic]]))


(defn handle-sub-topic-submit
  [project-id parent-id topic-name user-id]
  (fn [event]
    (.preventDefault event)
    (add-sub-topic project-id parent-id @topic-name user-id)
    (reset! topic-name "")))


(defn handle-topic-submit
  [topic-name project-id user-id]
  (fn [event]
    (.preventDefault event)
    (add-topic @topic-name project-id user-id)
    (reset! topic-name "")))
