(ns zuzhi.chaptify.core
  (:require
    [reagent.core :as r]
    [reagent.dom :as d]
    [reitit.core :as rt]
    [reitit.frontend.easy :as rt-easy]))


(def session (r/atom {:page :home :username "zuzhi" :tab nil}))

(def projects (r/atom [{:id 1 :name "fullstackopen" :status "normal" :progress 0}]))
(def archives (r/atom [{:id 2 :name "educated" :status "archived" :progress 100}]))


;; -------------------------
;; Views

(defn root
  []
  (let [{:keys [page username tab]} @session]
    (js/console.log @session)
    [:div.header
     [:a.product-name {:href (rt-easy/href :home)
                       :class (when (and (= page :home) (= tab nil)) "active")}
      [:b "chaptify"]]
     [:a {:href (rt-easy/href :profile {:username username} {:tab "projects"})
          :style {:padding-left 10}
          :class (when (and (= page :profile) (= tab "projects")) "active")}
      "projects"]
     [:a {:href (rt-easy/href :profile {:username username} {:tab "archives"})
          :style {:padding-left 10}
          :class (when (and (= page :profile) (= tab "archives")) "active")}
      "archives"]
     [:a {:href (rt-easy/href :profile {:username username}),
          :style {:padding-left 10}
          :class (when (and (= page :profile) (= tab nil)) "active")}
      "profile"]]))


(defn dashboard
  []
  [:div
   [root]
   (let [projects-count (count @projects)
         archives-count (count @archives)]
     [:div
      [:p "you have " (+ projects-count archives-count) " projects. "
       projects-count " active, " archives-count " archived."]])])


(defn projects-page
  [projects]
  (js/console.log projects)
  [:ul (for [p projects] [:li {:key (:id p)} (:name p)])])


(defn archives-page
  [archives]
  (js/console.log archives)
  [:ul (for [p archives] [:li {:key (:id p)} (:name p)])])


(defn profile
  [{:keys [username tab]}]
  [:div
   [root]
   (cond
     (= tab "projects") [projects-page @projects]
     (= tab "archives") [archives-page @archives]
     :else [:div [:p "hi, " username]])])


(def pages
  {:home #'dashboard
   :profile #'profile})


(defn page
  []
  (let [{:keys [page username tab]} @session]
    [(pages page) {:username username :tab tab}]))


;; -------------------------
;; Routing

(defn on-navigate
  [matched-route]
  (let [route-name (-> matched-route :data :name)
        username (-> matched-route :path-params :username)
        tab (-> matched-route :query-params :tab)]
    (swap! session assoc :page route-name :tab tab)
    (when username
      (swap! session assoc :username username))))


;; Define the routes
(def router
  (rt/router
    [["/" {:name :home}]
     ["/:username" {:name :profile}]]))


;; Initialize router and handle navigation events
(defn init-routes!
  []
  (rt-easy/start!
    router
    on-navigate
    {:use-fragment false}))


;; -------------------------
;; Initialize app

(defn ^:dev/after-load mount-root
  []
  (d/render [page] (.getElementById js/document "app")))


(defn ^:export ^:dev/once init!
  []
  (init-routes!)
  (mount-root))
