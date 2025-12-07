(ns web.response
  (:require [ring.util.response :refer [header response]]
            [web.layout :refer [head body]]
            [hiccup2.core :as h]))

(defn page-response
  ([hiccup-vector]
   (-> (h/html [:html head (body hiccup-vector)])
       str
       (response)
       (header "Content-Type" "text/html")))
  ([session-callback hiccup-vector]
   (session-callback (page-response hiccup-vector))))

(defn hx-response
  ([hiccup-vector]
   (-> (h/html hiccup-vector)
       str
       (response)
       (header "Content-Type" "text/html")))
  ([session-callback hiccup-vector]
   (session-callback (hx-response hiccup-vector))))
