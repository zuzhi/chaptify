(ns zuzhi.chaptify.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[chaptify starting]=-"))
   :start      (fn []
                 (log/info "\n-=[chaptify started successfully]=-"))
   :stop       (fn []
                 (log/info "\n-=[chaptify has shut down successfully]=-"))
   :middleware (fn [handler _] handler)
   :opts       {:profile :prod}})
