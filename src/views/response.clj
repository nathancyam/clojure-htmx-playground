(ns views.response
  (:require [ring.util.response :refer [header response]]
            [views.layout :refer [head body]]
            [hiccup2.core :as h]))

(defn render [tree-vector]
  (h/html tree-vector))

(defn page-response [content]
  (-> (render [:html head (body content)])
      str
      (response)
      (header "Content-Type" "text/html")))

(defn hx-response
  [hiccup-vector]
  (-> (h/html hiccup-vector)
      str
      (response)
      (header "Content-Type" "text/html")))

