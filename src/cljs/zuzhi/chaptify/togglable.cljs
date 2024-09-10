(ns zuzhi.chaptify.togglable
  (:require
    [reagent.core :as r]))


(defn togglable
  [{:keys [buttonLabel ref]} & children]
  (let [visible (r/atom false)
        toggle-visibility #(swap! visible not)
        set-visible (fn [v] (reset! visible v))]

    (when ref
      (reset! ref {:toggleVisibility toggle-visibility
                   :setVisible set-visible}))

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
