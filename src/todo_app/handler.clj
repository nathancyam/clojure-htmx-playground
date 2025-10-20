(ns todo-app.handler
  (:require
   [clojure.string :as str]
   [compojure.core :refer [defroutes DELETE GET PATCH POST PUT]]
   [compojure.route :as route]
   [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.util.response :refer [response status]]
   [todo-app.todos :as todos]
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
(defn get-todo [id]
  (try
    (if-let [todo (todos/get-todo-by-id (java.util.UUID/fromString id))]
      (json-response todo)
      (error-response "Todo not found" 404))
    (catch IllegalArgumentException _
      (error-response "Invalid todo ID format" 400))
    (catch Exception _
      (error-response "Failed to retrieve todo" 500))))

(defn create-todo [request]
  (prn (:params request))
  (try
    (let [todo-data (:body request)
          title (get todo-data "title")
          description (get todo-data "description")]
      (if (and title (not (str/blank? title)))
        (let [new-todo (todos/create-todo! {:title title
                                            :description description
                                            :completed false})]
          (json-response new-todo 201))
        (error-response "Title is required" 400)))
    (catch Exception _
      (error-response "Failed to create todo" 500))))

(defn update-todo [id request]
  (try
    (let [todo-data (:body request)
          uuid-id (parse-uuid id)]
      (if-let [_existing-todo (todos/get-todo-by-id uuid-id)]
        (let [updated-data (-> {}
                               (cond-> (contains? todo-data "title")
                                 (assoc :title (get todo-data "title")))
                               (cond-> (contains? todo-data "description")
                                 (assoc :description (get todo-data "description")))
                               (cond-> (contains? todo-data "completed")
                                 (assoc :completed (get todo-data "completed"))))]
          (if (seq updated-data)
            (let [updated-todo (todos/update-todo! uuid-id updated-data)]
              (json-response updated-todo))
            (error-response "No valid fields to update" 400)))
        (error-response "Todo not found" 404)))
    (catch IllegalArgumentException _
      (error-response "Invalid todo ID format" 400))
    (catch Exception _
      (error-response "Failed to update todo" 500))))

(defn delete-todo [id]
  (try
    (let [uuid-id (parse-uuid id)]
      (if (todos/get-todo-by-id uuid-id)
        (do
          (todos/delete-todo! uuid-id)
          (status (response "") 200))
        (error-response "Todo not found" 404)))
    (catch IllegalArgumentException _
      (error-response "Invalid todo ID format" 400))
    (catch Exception _
      (error-response "Failed to delete todo" 500))))

(defn my-page []
  (views.util/html-response (pages/home)))

(defn toggle-todo [id]
  (let [uid (parse-uuid id) todo (todos/get-todo-by-id uid)]
    (if (some? todo)
      (let [updated (todos/update-todo! uid {:completed (not (:todos/completed todo))})]
        (html-response (render (components/todo-component updated))))
      (response {:status 404}))))

(defn new-todo [data]
  (todos/create-todo! data)
  (-> (todos/get-all-todos)
      (pages/todo-list-hx)
      (html-response)))

;; Routes
(defroutes app-routes
  (GET "/example" [] (my-page))
  (GET "/todos" [] (-> (todos/get-all-todos)
                       (pages/todos)
                       (html-response)))
  (GET "/todos/:id" [id] (get-todo id))
  (POST "/todos" request (create-todo request))
  (POST "/todo" [title]
    (new-todo {:title title}))
  (PUT "/todos/:id" [id :as request] (update-todo id request))
  (PATCH "/todo/:id/status" [id] (toggle-todo id))
  (DELETE "/todos/:id" [id] (delete-todo id))
  (route/not-found {:error "Route not found"}))

;; Middleware stack
(def app
  (-> (wrap-reload #'app-routes)
      (wrap-json-body {:keywords? false})
      wrap-params
      wrap-json-response))
