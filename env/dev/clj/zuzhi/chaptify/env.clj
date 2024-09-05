(ns zuzhi.chaptify.env
  (:require
    [clojure.tools.logging :as log]
    [zuzhi.chaptify.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[chaptify starting using the development or test profile]=-"))
   :start      (fn []
                 (log/info "\n-=[chaptify started successfully using the development or test profile]=-"))
   :stop       (fn []
                 (log/info "\n-=[chaptify has shut down successfully]=-"))
   :middleware wrap-dev
   :opts       {:profile       :dev
                :persist-data? true}})
