(ns web-helpers
  (:require [ring.mock.request :as mock]))

(defn with-hx-headers [request]
  (-> request
      (mock/header "HX-Request" "true")
      (mock/header "HX-Boosted" "true")))

