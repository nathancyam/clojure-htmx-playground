(ns views.util
  (:require [ring.util.response :refer [header response]]
            [hiccup2.core :as h]))

(defn render [tree-vector]
  (h/html tree-vector))

(defn html-response
  [hiccup-html]
  (-> (str hiccup-html)
      (response)
      (header "Content-Type", "text/html")))

