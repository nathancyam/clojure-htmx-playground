(ns todo-app.handler
  (:require
   [clojure.string :as str]
   [compojure.core :refer [defroutes DELETE GET PATCH POST PUT context]]
   [ring.util.response :refer [response status]]
   [todo-app.db :as todos]
   [views.util :refer [html-response render]]
   [views.components :as components]
   [views.pages :as pages]))

(defn json-response [data & [status-code]]
  (-> (response data)
      (status (or status-code 200))))

(defn error-response [message & [status-code]]
  (-> (response {:error message})
      (status (or status-code 400))))

;; Route handlers
(defn get-todo [db id]
  (try
    (if-let [todo (todos/get-todo-by-id db (java.util.UUID/fromString id))]
      (json-response todo)
      (error-response "Todo not found" 404))
    (catch IllegalArgumentException _
      (error-response "Invalid todo ID format" 400))
    (catch Exception _
      (error-response "Failed to retrieve todo" 500))))

(defn create-todo [db request]
  (try
    (let [todo-data (:body request)
          title (get todo-data "title")
          description (get todo-data "description")]
      (if (and title (not (str/blank? title)))
        (let [new-todo (todos/create-todo! db {:title title
                                               :description description
                                               :completed false})]
          (json-response new-todo 201))
        (error-response "Title is required" 400)))
    (catch Exception _
      (error-response "Failed to create todo" 500))))

(defn update-todo [db id request]
  (try
    (let [todo-data (:body request)
          uuid-id (parse-uuid id)]
      (if-let [_existing-todo (db todos/get-todo-by-id uuid-id)]
        (let [updated-data (-> {}
                               (cond-> (contains? todo-data "title")
                                 (assoc :title (get todo-data "title")))
                               (cond-> (contains? todo-data "description")
                                 (assoc :description (get todo-data "description")))
                               (cond-> (contains? todo-data "completed")
                                 (assoc :completed (get todo-data "completed"))))]
          (if (seq updated-data)
            (let [updated-todo (todos/update-todo! db uuid-id updated-data)]
              (json-response updated-todo))
            (error-response "No valid fields to update" 400)))
        (error-response "Todo not found" 404)))
    (catch Exception _
      (error-response "Failed to update todo" 500))))

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
        (html-response (render (components/todo-component updated))))
      (response {:status 404}))))

(defn new-todo [db data]
  (todos/create-todo! db data)
  (-> db
      (todos/get-all-todos)
      (pages/todo-list-hx)
      (html-response)))

(defroutes todo-app-id-routes
  (GET "/" [id :as {db :db}] (get-todo db id))
  (POST "/" [request :as {db :db}] (create-todo db request))
  (PUT "/" [id :as request :as {db :db}] (update-todo db id request))
  (PATCH "/status" [id :as {db :db}] (toggle-todo db id))
  (DELETE "/" [id :as {db :db}] (delete-todo db id)))

;; Routes
(defroutes routes
  (GET "/todos" [:as {db :db}] (-> db (todos/get-all-todos)
                                   (pages/todos)
                                   (html-response)))
  (context "/todos/:id" [] todo-app-id-routes)
  (POST "/todo" [title :as {db :db}]
    (new-todo db {:title title})))
