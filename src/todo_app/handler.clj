(ns todo-app.handler
  (:require
   [compojure.core :refer [defroutes DELETE GET PATCH POST context]]
   [ring.util.response :refer [response status]]
   [todo-app.db :as todos]
   [todo-app.views :as v]
   [views.util :refer [html-response render]]))

(defn error-response [message & [status-code]]
  (-> (response {:error message})
      (status (or status-code 400))))

(defn delete-todo [db id]
  (try
    (let [uuid-id (parse-uuid id)]
      (if (todos/get-todo-by-id db uuid-id)
        (do
          (todos/delete-todo! db uuid-id)
          (status (response "") 200))
        (error-response "Todo not found" 404)))
    (catch IllegalArgumentException _
      (error-response "Invalid todo ID format" 400))
    (catch Exception _
      (error-response "Failed to delete todo" 500))))

(defn toggle-todo [db id]
  (let [uid (parse-uuid id) todo (todos/get-todo-by-id db uid)]
    (if (some? todo)
      (let [updated (todos/update-todo! db uid {:completed (not (:todos/completed todo))})]
        (html-response (render (v/todo-component updated))))
      (response {:status 404}))))

(defn new-todo [db data]
  (todos/create-todo! db data)
  (-> db
      (todos/get-all-todos)
      (v/todo-list-hx)
      (html-response)))

(defroutes todo-routes
  (PATCH "/status" [id :as {db :db}] (toggle-todo db id))
  (DELETE "/" [id :as {db :db}] (delete-todo db id)))

;; Routes
(defroutes routes
  (GET "/todos" [:as {db :db}] (-> db (todos/get-all-todos)
                                   (v/todos)
                                   (html-response)))
  (context "/todos/:id" [] todo-routes)
  (POST "/todo" [title :as {db :db}]
    (new-todo db {:title title})))
