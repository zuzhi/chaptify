(ns zuzhi.chaptify.events.editor
  (:require
    [zuzhi.chaptify.db :refer [init-project]]))


(defn handle-editor-save
  [project topics user-id]
  (init-project @project topics user-id))
