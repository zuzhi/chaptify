(ns zuzhi.chaptify.util)

(defn has-matching-parent?
  [topic-id sub-topic]
  (some #(= (:id %) topic-id) (:parent sub-topic)))


(defn transform-topic
  [topic sub-topics]
  (let [children (filter #(has-matching-parent? (:id topic) %) sub-topics)]
    (assoc topic :children (for [t children]
                             (transform-topic t sub-topics)))))

(defn transform-project
  [{:keys [id name status progress created_at topics]}]
  (let [direct-topics (filter #(= (:parent %) []) topics)
        sub-topics (filter #(not= (:parent %) []) topics)]
    {:id id
     :name name
     :status status
     :progress progress
     :created_at created_at
     :topics (for [t direct-topics]
               (transform-topic t sub-topics))}))
