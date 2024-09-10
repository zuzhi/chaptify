(ns zuzhi.chaptify.web.routes.pages
  (:require
    [integrant.core :as ig]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [zuzhi.chaptify.web.middleware.exception :as exception]
    [zuzhi.chaptify.web.pages.layout :as layout]))


(defn wrap-page-defaults
  []
  (let [error-page (layout/error-page
                     {:status 403
                      :title "Invalid anti-forgery token"})]
    #(wrap-anti-forgery % {:error-response error-page})))


(defn home
  [request]
  (layout/render request "home.html"))


;; Routes
(defn page-routes
  [_opts]
  [["/**" {:get home}]])


(def route-data
  {:middleware
   [;; Default middleware for pages
    (wrap-page-defaults)
    ;; query-params & form-params
    parameters/parameters-middleware
    ;; encoding response body
    muuntaja/format-response-middleware
    ;; exception handling
    exception/wrap-exception]})


(derive :reitit.routes/pages :reitit/routes)


(defmethod ig/init-key :reitit.routes/pages
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (layout/init-selmer! opts)
  (fn [] [base-path route-data (page-routes opts)]))
