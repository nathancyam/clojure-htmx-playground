(ns views.response
  (:require [ring.util.response :refer [header response]]
            [views.layout :refer [head body]]
            [hiccup2.core :as h]))

(defn page-response [hiccup-vector]
  (-> (h/html [:html head (body hiccup-vector)])
      str
      (response)
      (header "Content-Type" "text/html")))

(defn hx-response
  [hiccup-vector]
  (-> (h/html hiccup-vector)
      str
      (response)
      (header "Content-Type" "text/html")))

