(ns web.response
  (:require [web.layout :refer [head body]]
            [hiccup2.core :as h]))

(defn page-response
  [hiccup-vector]
  (str (h/html [:html head (body hiccup-vector)])))

(defn hx-response
  [hiccup-vector]
  (str (h/html hiccup-vector)))
