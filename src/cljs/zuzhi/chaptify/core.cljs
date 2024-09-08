(ns zuzhi.chaptify.core
  (:require
    ["@instantdb/react" :as instantdb]
    [reagent.core :as r]
    [reagent.dom.client :as rdc]
    [reitit.core :as rt]
    [reitit.frontend.easy :as rt-easy]))


(def session (r/atom {:page :home :username "zuzhi" :tab nil}))

(def projects (r/atom [{:id 1 :name "fullstackopen" :status "normal" :progress 0}]))
(def archives (r/atom [{:id 2 :name "educated" :status "archived" :progress 100}]))


;; ID for app: chaptify
(def app-id "71eb5a33-d683-4c63-8638-109df239ec0a")


;; Initialize the InstantDB
(def db (instantdb/init #js {:appId app-id}))


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
      [:div (str "Error fetching data: " (.-message error))]

      :else
      [:p "you have " (+ projects-count archives-count) " projects. "
       projects-count " active, " archives-count " archived."])))


(defn fn-instant-dashboard
  []
  [:div
   [root]
   [:f> instant-dashboard]])


(defn projects-page
  [projects]
  (js/console.log projects)
  [:ul (for [p projects] [:li {:key (:id p)} (:name p)])])


(defn instant-projects-page
  []
  (let [query {:projects {:$ {:where {:status 'normal}}}}
        result (.useQuery db (clj->js query))
        {:keys [isLoading error data]} (js->clj result :keywordize-keys true)
        projects (or (:projects data) [])]
    (cond
      isLoading
      [:div "loading"]

      error
      [:div (str "Error fetching data: " (.-message error))]

      :else
      [:ul
       (for [p projects] [:li {:key (:id p)} (:name p)])])))


(defn archives-page
  [archives]
  (js/console.log archives)
  [:ul (for [p archives] [:li {:key (:id p)} (:name p)])])


(defn instant-archives-page
  []
  (let [query {:projects {:$ {:where {:status 'archived}}}}
        result (.useQuery db (clj->js query))
        {:keys [isLoading error data]} (js->clj result :keywordize-keys true)
        projects (or (:projects data) [])]
    (cond
      isLoading
      [:div "loading"]

      error
      [:div (str "Error fetching data: " (.-message error))]

      :else
      [:ul
       (for [p projects] [:li {:key (:id p)} (:name p)])])))


(defn profile
  [{:keys [username tab]}]
  [:div
   [root]
   (cond
     (= tab "projects") [:f> instant-projects-page]
     (= tab "archives") [:f> instant-archives-page]
     :else [:div [:p "hi, " username]])])


(def pages
  {:home #'fn-instant-dashboard
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

(defonce app (rdc/create-root (js/document.getElementById "app")))


(defn ^:dev/after-load mount-root
  []
  (.render app (r/as-element [page])))


(defn ^:export ^:dev/once init!
  []
  (init-routes!)
  (mount-root))
