(ns zuzhi.chaptify.core
  (:require
    [clojure.string :as str]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reagent.dom.client :as rdc]
    [reitit.coercion.schema :as rtcs]
    [reitit.frontend :as rtf]
    [reitit.frontend.controllers :as rtfc]
    [reitit.frontend.easy :as rtf-easy]
    [zuzhi.chaptify.components.archives :refer [ArchivesPage]]
    [zuzhi.chaptify.components.projects :refer [ProjectsPage]]
    [zuzhi.chaptify.db :refer [db]]))


;; re-frame
;; events
(def rfdb {:user nil :match nil})


(rf/reg-event-db
  :initialize
  (fn [_ _]
    rfdb))


(rf/reg-event-db
  :set-user
  (fn [rfdb [_ user]]
    (assoc rfdb :user user)))


;; subs
(rf/reg-sub
  :user
  (fn [rfdb _]
    (:user rfdb)))


(rf/reg-sub
  :match
  (fn [rfdb _]
    (:match rfdb)))


;; -------------------------
;; Views
(defonce state (r/atom {:match nil}))


(defn nav
  []
  (let [{:keys [match]} @state
        user (rf/subscribe [:user])
        route-data (:data match)
        route-name (:name route-data)
        user-email (:email @user)
        username (first (str/split user-email #"@"))
        {:keys [path query]} (:parameters match)
        tab (:tab query)]
    (js/console.log "match:" match)
    (js/console.log ::profile)
    [:div.header
     [:a.product-name {:href (rtf-easy/href ::home)
                       :class (when (= route-name ::home) "active")}
      [:b "chaptify"]]
     [:a {:href (rtf-easy/href ::profile {:username username} {:tab "projects"})
          :class (when (and (= route-name ::profile) (= tab "projects")) "active")
          :style {:padding-left 8}}
      "projects"]
     [:a {:href (rtf-easy/href ::profile {:username username} {:tab "archives"})
          :class (when (and (= route-name ::profile) (= tab "archives")) "active")
          :style {:padding-left 8}}
      "archives"]
     [:a {:href (rtf-easy/href ::profile {:username username})
          :class (when (and (= route-name ::profile) (= tab nil)) "active")
          :style {:padding-left 8}}
      "profile"]]))


(defn dashboard
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


(defn logout
  []
  (-> (.-auth db)
      (.signOut)
      (.then (fn []
               (rf/dispatch [:set-user nil])))))


(defn footer
  []
  (let [user (rf/subscribe [:user])
        user-email (:email @user)
        username (first (str/split user-email #"@"))]
    [:div
     [:span {:style {:font-size ".8em" :color "#828282"}} (str username " |")]
     [:button {:style {:padding-left 8}
               :on-click #(logout)}
      "logout"]]))


(defn home-page
  []
  [:div
   [nav]
   [:f> dashboard]
   [footer]])


(defn profile
  [username tab]
  [:div
   [nav]
   (cond
     (= tab "projects") [:f> ProjectsPage]
     (= tab "archives") [:f> ArchivesPage]
     :else [:div [:p "hi, " username]])
   [footer]])


(defn page-404
  []
  [:div
   [:p "page not found"]
   [:button {:on-click #(rtf-easy/push-state ::home)} "<- home"]])


(defn user-page
  [match]
  (let [{:keys [path query]} (:parameters match)
        {:keys [username]} path
        {:keys [tab]} query
        user (rf/subscribe [:user])
        user-email (:email @user)
        current-username (first (str/split user-email #"@"))]
    (js/console.log "match:" match)
    (if (= username current-username)
      [profile username tab]
      [page-404])))


(defn handle-email-submit
  [set-sent-email email]
  (fn [e]
    (.preventDefault e)
    (when (seq email)
      (set-sent-email email)
      (-> (.-auth db)
          (.sendMagicCode (clj->js {:email email}))
          (.then (fn [data]
                   (js/console.log data)))
          (.catch (fn [err]
                    (.alert js/window (str "uh oh: " (.-message (.-body err))))
                    (set-sent-email "")))))))


(defn email
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
        (.then (fn [data]
                 (js/console.log data)
                 (let [{:keys [user]} (js->clj data :keywordize-keys true)]
                   (rf/dispatch [:set-user user]))))
        (.catch (fn [err]
                  (js/console.log err)
                  (.alert js/window (str "uh oh: " (.-message (.-body err))))
                  (set-code ""))))))


(defn magic-code
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


(defn login-view
  []
  (let [sent-email (r/atom "")
        code (r/atom "")]
    (fn []
      [:div
       (if (empty? @sent-email)
         [email {:set-sent-email #(reset! sent-email %)}]
         [magic-code {:set-code #(reset! code %)} @sent-email])])))


(defn main-view
  []
  (let [{:keys [match]} @state
        route-data (:data match)
        result (.useAuth db)
        {:keys [isLoading user error]} (js->clj result :keywordize-keys true)]
    (js/console.log "user:" user)
    (js/console.log "match:" match)
    (cond
      isLoading
      [:div "loading"]

      error
      [:div (str "uh oh: " (.-message error))]

      user
      (if match
        (let [view (:view route-data)
              view-name (:name route-data)]
          (rf/dispatch [:set-user user])
          (if (= view-name ::login)
            (do
              (rtf-easy/push-state ::home)
              [home-page match])
            [view match]))
        [page-404])

      :else
      (if match
        (let [view (:view route-data)]
          (if (:public? route-data)
            [view match]
            [login-view]))
        [login-view]))))


;; -------------------------
;; Routing

(defn log-fn
  [& params]
  (fn [_]
    (apply js/console.log params)))


(def routes
  ["/"
   [""
    {:name ::home
     :view home-page}]
   ["login"
    {:name ::login
     :view login-view
     :public? true}]
   [":username"
    {:name ::profile
     :view user-page}]])


(def router
  (rtf/router
    routes
    {:conflicts nil
     :data {:coercion rtcs/coercion
            :public? false}}))


(defn init-router
  []
  (rtf-easy/start!
    router
    ;; on-navigate
    (fn [new-match]
      (swap! state (fn [state]
                     (if new-match
                       ;; Only run the controllers, which are likely to call authenticated APIs,
                       ;; if user has been authenticated.
                       ;; Alternative solution could be to always run controllers,
                       ;; check authentication status in each controller, or check authentication status in API calls.
                       (if (:user state)
                         (assoc state :match (assoc new-match :controllers (rtfc/apply-controllers (:controllers (:match state)) new-match)))
                         (assoc state :match new-match))))))
    {:use-fragment false}))


;; -------------------------
;; Initialize app

(defonce root (rdc/create-root (js/document.getElementById "root")))


(defn ^:dev/after-load mount-root
  []
  (rdc/render root [:f> main-view]))


(defn ^:export ^:dev/once init!
  []
  (rf/dispatch-sync [:initialize])
  (init-router)
  (mount-root))
