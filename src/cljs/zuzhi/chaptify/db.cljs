(ns zuzhi.chaptify.db
  (:require
    ["@instantdb/react" :refer [init tx id]]))


;; ID for app: chaptify
(def app-id "71eb5a33-d683-4c63-8638-109df239ec0a")


;; Initialize the InstantDB
(def db (init #js {:appId app-id}))


;; topics
(defn get-topics
  []
  (let [query {:topics {}}
        result (.useQuery db (clj->js query))]
    result))


(defn add-topic
  [name project-id user-id]
  (let [id (id)
        topic (aget (.-topics tx) id)
        update-data (clj->js {:name name
                              :status "pending"
                              :creatorId user-id})
        project (aget (.-projects tx) project-id)
        link-data (clj->js {:topics id})]
    (when topic
      (js/console.log update-data)
      (.transact db (.update topic update-data))
      (.transact db (.link project link-data)))))


(defn add-sub-topic
  [project-id parent-id name user-id]
  (let [id (id)
        topic (aget (.-topics tx) id)
        update-data (clj->js {:name name
                              :status "pending"
                              :creatorId user-id})
        project (aget (.-projects tx) project-id)
        project-link-data (clj->js {:topics id})
        parent (aget (.-topics tx) parent-id)
        parent-link-data (clj->js {:children id})]
    (when topic
      (.transact db (.update topic update-data))
      (.transact db (.link project project-link-data))
      (.transact db (.link parent parent-link-data)))))


(defn update-topic-status
  [topic-id status]
  (let [topic (aget (.-topics tx) topic-id)
        update-data (clj->js {:status status})]
    (when topic
      (.transact db (.update topic update-data)))))


;; projects
(defn build-nested-children
  [levels]
  (if (pos? levels)
    {:children (build-nested-children (dec levels)) :parent {}}
    {}))


(defn get-projects
  ([] (get-projects "normal"))
  ([status] (get-projects status false))
  ([status with-topics?]
   (let [query {:projects (cond-> {:$ {:where {:status status}}}
                            ;; with-topics? (assoc :topics (build-nested-children 10)))}
                            with-topics? (assoc :topics {:parent {}}))}
         result (.useQuery db (clj->js query))]
     result)))


(defn add-project
  [name user-id]
  (let [id (id)
        project (aget (.-projects tx) id)
        update-data (clj->js {:name name
                              :status "normal"
                              :createdAt (.now js/Date)
                              :creatorId user-id})]
    (when project
      (js/console.log update-data)
      (.transact db (.update project update-data)))))


(defn rename-project
  [id name]
  (let [project (aget (.-projects tx) id)
        update-data (clj->js {:name name})]
    (when project
      (.transact db (.update project update-data)))))


(defn delete-topic
  ([topic-id] (delete-topic topic-id []))
  ([topic-id children]
   (let [topic (aget (.-topics tx) topic-id)]
     (when topic
       (.transact db (.delete topic))
       (doseq [t children]
         (delete-topic (:id t) (:children t)))))))


(defn delete-project
  [{:keys [id topics]}]
  (let [project (aget (.-projects tx) id)]
    (when project
      (.transact db (.delete project))
      (doseq [t topics]
        (delete-topic (:id t))))))


(defn archive-project
  [project-id]
  (let [project (aget (.-projects tx) project-id)
        update-data (clj->js {:status "archived"})]
    (when project
      (.transact db (.update project update-data)))))


(defn unarchive-project
  [project-id]
  (let [project (aget (.-projects tx) project-id)
        update-data (clj->js {:status "normal"})]
    (when project
      (.transact db (.update project update-data)))))
