(ns agoraopus-random-walk.ring
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as response]))

(defroutes app-routes
           (GET "/" [] (response/redirect "/index.html"))
           (route/resources "/" {:root "public"})
           (route/not-found "Not Found"))

;; NOTE: wrap reload isn't needed when the clj sources are watched by figwheel
;; but it's very good to know about
(def handler (wrap-reload (wrap-defaults #'app-routes site-defaults)))
