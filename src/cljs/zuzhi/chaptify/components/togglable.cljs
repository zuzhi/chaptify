(ns zuzhi.chaptify.components.togglable
  (:require
    [reagent.core :as r]))


(defn Togglable
  [{:keys [buttonLabel ref]} & children]
  (let [visible (r/atom false)
        toggle-visibility #(swap! visible not)
        set-visible (fn [v]
                      (reset! visible v))]

    (when ref
      (reset! ref {:toggle-visibility toggle-visibility
                   :set-visible set-visible}))

    (fn []
      [:div
       (when buttonLabel
         [:div {:style {:display (when @visible "none")}}
          [:button {:on-click toggle-visibility}
           buttonLabel]])
       [:div.togglableContent {:style {:display (when-not @visible "none")}}
        children
        [:button {:on-click toggle-visibility}
         "cancel"]]])))
