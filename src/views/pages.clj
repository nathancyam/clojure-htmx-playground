(ns views.pages
  (:require [views.components :as c]
            [views.util :refer [render]]
            [clojure.tools.logging :as log]))

(def head
  [:head [:title "example app"]
   [:script {:src "https://cdn.jsdelivr.net/npm/htmx.org@2.0.7/dist/htmx.min.js" :defer true}]
   [:script {:src "https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4" :defer true}]])

(defn- body [content]
  [:body
   [:div
    {:class "min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 py-8 px-4"}
    [:div {:class "max-w-2xl mx-auto"} [:div {:class "bg-white rounded-lg shadow-lg p-6"} content]]]])

(defn layout [content]
  (render [:html head (body content)]))

(defn home []
  (layout [:span {:class "example"} "Ahhh"]))

(defn todo-list [todos]
  (log/info "hello for list")
  [:section.space-y-2 {:id "todo-list"}
   c/new-todo
   (for [todo todos] (c/todo-component todo))])

(defn todo-list-hx [todos]
  (render (todo-list todos)))

(defn todos [todos]
  (layout (todo-list todos)))
