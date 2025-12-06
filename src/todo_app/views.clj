(ns todo-app.views
  (:require
   [web.components :refer [button-color]]))

(defn todo-component [todo]
  (let [id (:todos/id todo)
        html-id (str "todo-" id)
        completed? (:todos/completed todo)
        title (:todos/title todo)]
    [:div {:id html-id
           :class "flex items-center gap-3 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"}
     [:input
      {:type "checkbox"
       :class "w-5 h-5 text-blue-500 rounded cursor-pointer"
       :checked completed?
       :hx-patch (str "/todos/" id "/status")}]
     [:span {:class "flex-1 text-gray-700"} title]
     [:button {:type "button"
               :class "text-red-500 hover:text-red-700 transition-colors text-xl"
               :hx-target (str "#" html-id)
               :hx-swap "outerHTML"
               :hx-delete (str "/todos/" id)} "Delete"]]))

(def new-todo
  [:div {:class "flex gap-2 mb-6"}
   [:input {:name "title"
            :class "flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"}]
   [:button {:class (button-color "blue") :hx-post "/todo" :hx-target "#todo-list" :hx-swap "outerHTML" :hx-include "previous input[name='title']"} "Save"]])

(defn todo-list [todos]
  [:section.space-y-2 {:id "todo-list"}
   new-todo
   (for [todo todos] (todo-component todo))])
