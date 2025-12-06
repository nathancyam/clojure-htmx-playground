(ns views.layout
  (:require [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(def head
  [:head [:title "example app"]
   [:script {:src "https://cdn.jsdelivr.net/npm/htmx.org@2.0.7/dist/htmx.min.js" :defer true}]
   [:script {:src "https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4" :defer true}]
   [:meta {:csrf-token *anti-forgery-token*}]])

(defn body [content]
  [:body
   [:div
    {:class "min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 py-8 px-4"}
    [:main {:class "max-w-2xl mx-auto" :id "main-wrapper"} content]]])
