(ns zuzhi.chaptify.core
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reagent.dom.client :as rdc]
   [reitit.frontend :as rt]
   [reitit.frontend.easy :as rt-easy]
   [zuzhi.chaptify.components.archives :refer [ArchivesPage]]
   [zuzhi.chaptify.components.projects :refer [ProjectsPage]]
   [zuzhi.chaptify.db :refer [db]]))


;; re-frame
;; events
(def state {:user {}})


(rf/reg-event-db
  :initialize
  (fn [_ _]
    state))


(rf/reg-event-db
  :set-user
  (fn [state [_ user]]
    (assoc state :user user)))


;; subs
(rf/reg-sub
  :user
  (fn [state _]
    (:user state)))


;; -------------------------
;; Views

(def app-state
  (r/atom {:current-view-name nil
           :current-view nil
           :route-params nil
           :query-params nil}))


(defn Nav
  []
  (let [{:keys [current-view-name query-params user-email]} @app-state
        username (first (str/split user-email #"@"))
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


(defn InstantDashboard
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


(defn Footer
  []
  (let [user-email (:user-email @app-state)
        username (first (str/split user-email #"@"))]
    [:div
     [:span {:style {:font-size ".8em" :color "#828282"}} (str username " |")]
     [:button {:style {:padding-left 8}
               :on-click #(-> (.-auth db)
                              (.signOut)
                              (.then (fn []
                                       (swap! app-state assoc
                                              :user-email nil
                                              :user-id nil)
                                       (rt-easy/push-state ::login))))}
      "logout"]]))


(defn Dashboard
  []
  [:div
   [Nav]
   [:f> InstantDashboard]
   [Footer]])


(defn Profile
  [username tab]
  [:div
   [Nav]
   (cond
     (= tab "projects") [:f> ProjectsPage]
     (= tab "archives") [:f> ArchivesPage]
     :else [:div [:p "hi, " username]])
   [Footer]])


(defn ProfilePage
  [{:keys [username]} {:keys [tab]}]
  [Profile username tab])


(defn handle-email-submit
  [set-sent-email email]
  (fn [e]
    (.preventDefault e)
    (when (seq email)
      (set-sent-email email)
      (-> (.-auth db)
          (.sendMagicCode (clj->js {:email email}))
          (.catch (fn [err]
                    (.alert js/window (str "uh oh: " (.-message (.-body err))))
                    (set-sent-email "")))))))


(defn Email
  [{:keys [set-sent-email]}]
  (let [email (r/atom "")]
    (fn []
      [:form {:style {}
              :on-submit (handle-email-submit set-sent-email @email)}
       [:h2 "let's log you in!"]
       [:div
        [:input {:placeholder "enter your email"
                 :type "email"
                 :value @email
                 :on-change #(reset! email (-> % .-target .-value))}]
        [:button {:type "submit"
                  :style {:padding-left 8}}
         "send code"]]])))


(defn handle-code-submit
  [set-code sent-email code]
  (fn [e]
    (.preventDefault e)
    (-> (.-auth db)
        (.signInWithMagicCode (clj->js {:email sent-email :code @code}))
        (.catch (fn [err]
                  (.alert js/window (str "uh oh: " (.-message (.-body err))))
                  (set-code ""))))))


(defn MagicCode
  [{:keys [set-code]} sent-email]
  (let [code (r/atom "")]
    (fn []
      [:form {:on-submit (handle-code-submit set-code sent-email code)}
       [:h2 "okay, we sent you an email! what was the code?"]
       [:div
        [:input {:type "text"
                 :value @code
                 :on-change #(reset! code (-> % .-target .-value))}]
        [:button {:type "submit"
                  :style {:padding-left 8}}
         "verify"]]])))


(defn LoginPage
  []
  (let [sent-email (r/atom "")
        code (r/atom "")]
    (fn []
      [:div
       (if (empty? @sent-email)
         [Email {:set-sent-email #(reset! sent-email %)}]
         [MagicCode {:set-code #(reset! code %)} @sent-email])])))


(defn BaseView
  []
  (let [current-view (:current-view @app-state)
        route-params (:route-params @app-state)
        query-params (:query-params @app-state)
        result (.useAuth db)
        {:keys [isLoading user error]} (js->clj result :keywordize-keys true)]
    (cond
      isLoading
      [:div "loading"]

      error
      [:div (str "uh oh!: " (.-message error))]

      user
      (let [email (:email user)
            user-id (:id user)]
        (swap! app-state assoc
               :user-email email
               :user-id user-id)
        (rf/dispatch [:set-user user])
        (if current-view
          [current-view route-params query-params]
          [:div "page not found."]))

      :else
      [LoginPage])))


;; -------------------------
;; Routing

(def routes
  [["/" {:name ::home
         :view Dashboard}]
   ["/login" {:name ::login
              :view LoginPage
              :conflicting true}]
   ["/:username" {:name ::profile
                  :view ProfilePage
                  :conflicting true}]])


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
  (rdc/render root [:f> BaseView]))


(defn ^:export ^:dev/once init!
  []
  (rf/dispatch-sync [:initialize])
  (init-router)
  (mount-root))
