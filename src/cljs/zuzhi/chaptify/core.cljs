(ns zuzhi.chaptify.core
  (:require
    [reagent.core :as r]
    [reagent.dom.client :as rdc]
    [reitit.frontend :as rt]
    [reitit.frontend.easy :as rt-easy]
    [zuzhi.chaptify.components.archives :refer [ArchivesPage]]
    [zuzhi.chaptify.components.projects :refer [ProjectsPage]]
    [zuzhi.chaptify.db :refer [db]]))


;; -------------------------
;; Views

(def app-state (r/atom {:current-view-name nil :current-view nil :route-params nil :query-params nil}))


(defn base-view
  []
  (let [current-view (:current-view @app-state)
        route-params (:route-params @app-state)
        query-params (:query-params @app-state)]
    (if current-view
      [current-view route-params query-params]
      [:div "Page not found."])))


(defn nav
  []
  (let [{:keys [current-view-name query-params]} @app-state
        username "zuzhi"
        tab (:tab query-params)]
    [:div.header
     [:a.product-name {:href (rt-easy/href ::home)
                       :class (when (and (= current-view-name ::home) (= tab nil)) "active")}
      [:b "chaptify"]]
     [:a {:href (rt-easy/href ::profile {:username username} {:tab "projects"})
          :style {:padding-left 8}
          :class (when (and (= current-view-name ::profile) (= tab "projects")) "active")}
      "projects"]
     [:a {:href (rt-easy/href ::profile {:username username} {:tab "archives"})
          :style {:padding-left 8}
          :class (when (and (= current-view-name ::profile) (= tab "archives")) "active")}
      "archives"]
     [:a {:href (rt-easy/href ::profile {:username username}),
          :style {:padding-left 8}
          :class (when (and (= current-view-name ::profile) (= tab nil)) "active")}
      "profile"]]))


(defn instant-dashboard
  []
  (let [query {:projects {}}
        result (.useQuery db (clj->js query))
        {:keys [isLoading error data]} (js->clj result :keywordize-keys true)
        projects (or (:projects data) [])
        projects-count (count (filter #(= (:status %) "normal") projects))
        archives-count (count (filter #(= (:status %) "archived") projects))]
    (cond
      isLoading
      [:div "loading"]

      error
      [:div (str "error fetching data: " (.-message error))]

      :else
      [:div
       [:p "you have " (+ projects-count archives-count) " projects. "
        projects-count " active, " archives-count " archived."]])))


(defn footer
  [username]
  [:div
   [:span {:style {:font-size ".8em" :color "#828282"}} (str username " |")]
   [:button {:style {:padding-left 8}}
    "logout"]])


(defn fn-instant-dashboard
  []
  [:div
   [nav]
   [:f> instant-dashboard]
   [footer "zuzhi"]])


(defn profile
  [username tab]
  [:div
   [nav]
   (cond
     (= tab "projects") [:f> ProjectsPage]
     (= tab "archives") [:f> ArchivesPage]
     :else [:div [:p "hi, " username]])
   [footer "zuzhi"]])


(defn profile-page
  [{:keys [username]} {:keys [tab]}]
  [profile username tab])


;; -------------------------
;; Routing

(def routes
  [["/" {:name ::home
         :view fn-instant-dashboard}]
   ["/:username" {:name ::profile
                  :view profile-page}]])


(def router
  (rt/router routes))


(defn on-navigate
  [match _]
  (let [view-name (:name (:data match))
        view (:view (:data match))
        route-params (:path-params match)
        query-params (:query-params match)]
    (swap! app-state assoc
           :current-view-name view-name
           :current-view view
           :route-params route-params
           :query-params query-params)))


(defn init-router
  []
  (rt-easy/start!
    router
    on-navigate
    {:use-fragment false}))


;; -------------------------
;; Initialize app

(defonce root (rdc/create-root (js/document.getElementById "root")))


(defn ^:dev/after-load mount-root
  []
  (when (= (:current-view-name @app-state) nil)
    (init-router)) ; hot-reload reset app-state, init-router again
  (rdc/render root [base-view]))


(defn ^:export ^:dev/once init!
  []
  (init-router)
  (mount-root))
