(ns todo-app.handler
  (:require [compojure.core :refer [defroutes GET POST PUT DELETE]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response status]]
            [todo-app.db :as db]
            [clojure.string :as str]))

(defn json-response [data & [status-code]]
  (-> (response data)
      (status (or status-code 200))))

(defn error-response [message & [status-code]]
  (-> (response {:error message})
      (status (or status-code 400))))

;; Route handlers
(defn get-todos []
  (try
    (let [todos (db/get-all-todos)]
      (json-response todos))
    (catch Exception e
      (error-response "Failed to retrieve todos" 500))))

(defn get-todo [id]
  (try
    (if-let [todo (db/get-todo-by-id (java.util.UUID/fromString id))]
      (json-response todo)
      (error-response "Todo not found" 404))
    (catch IllegalArgumentException _
      (error-response "Invalid todo ID format" 400))
    (catch Exception _
      (error-response "Failed to retrieve todo" 500))))

(defn create-todo [request]
  (try
    (let [todo-data (:body request)
          title (get todo-data "title")
          description (get todo-data "description")]
      (if (and title (not (str/blank? title)))
        (let [new-todo (db/create-todo! {:title title
                                         :description description
                                         :completed false})]
          (json-response new-todo 201))
        (error-response "Title is required" 400)))
    (catch Exception e
      (error-response "Failed to create todo" 500))))

(defn update-todo [id request]
  (try
    (let [todo-data (:body request)
          uuid-id (java.util.UUID/fromString id)]
      (if-let [existing-todo (db/get-todo-by-id uuid-id)]
        (let [updated-data (-> {}
                               (cond-> (contains? todo-data "title")
                                 (assoc :title (get todo-data "title")))
                               (cond-> (contains? todo-data "description")
                                 (assoc :description (get todo-data "description")))
                               (cond-> (contains? todo-data "completed")
                                 (assoc :completed (get todo-data "completed"))))]
          (if (seq updated-data)
            (let [updated-todo (db/update-todo! uuid-id updated-data)]
              (json-response updated-todo))
            (error-response "No valid fields to update" 400)))
        (error-response "Todo not found" 404)))
    (catch IllegalArgumentException e
      (error-response "Invalid todo ID format" 400))
    (catch Exception e
      (error-response "Failed to update todo" 500))))

(defn delete-todo [id]
  (try
    (let [uuid-id (java.util.UUID/fromString id)]
      (if (db/get-todo-by-id uuid-id)
        (do
          (db/delete-todo! uuid-id)
          (json-response {:message "Todo deleted successfully"}))
        (error-response "Todo not found" 404)))
    (catch IllegalArgumentException e
      (error-response "Invalid todo ID format" 400))
    (catch Exception e
      (error-response "Failed to delete todo" 500))))

;; Routes
(defroutes app-routes
  (GET "/todos" [] (get-todos))
  (GET "/todos/:id" [id] (get-todo id))
  (POST "/todos" request (create-todo request))
  (PUT "/todos/:id" [id :as request] (update-todo id request))
  (DELETE "/todos/:id" [id] (delete-todo id))
  (route/not-found {:error "Route not found"}))

;; Middleware stack
(def app
  (-> app-routes
      (wrap-json-body {:keywords? false})
      wrap-json-response))
