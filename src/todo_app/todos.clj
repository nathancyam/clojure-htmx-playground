(ns todo-app.todos
  (:require [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [todo-app.db :refer [get-db]]
            [honey.sql.helpers :as h :refer [select from order-by where insert-into values returning delete-from]]))

;; Database operations
(defn get-all-todos []
  (let [query (-> (select :*)
                  (from :todos)
                  (order-by [:created_at :desc])
                  sql/format)]
    (jdbc/execute! (get-db) query)))

(defn get-todo-by-id [id]
  (let [query (-> (select :*)
                  (from :todos)
                  (where [:= :id id])
                  sql/format)]
    (first (jdbc/execute! (get-db) query))))

(defn create-todo! [todo-data]
  (let [query (-> (insert-into :todos)
                  (values [todo-data])
                  (returning :*)
                  sql/format)]
    (first (jdbc/execute! (get-db) query))))

(defn update-todo! [id todo-data]
  (let [query (-> (h/update :todos)
                  (h/set todo-data)
                  (where [:= :id id])
                  (returning :*)
                  sql/format)]
    (first (jdbc/execute! (get-db) query))))

(defn delete-todo! [id]
  (let [query (-> (delete-from :todos)
                  (where [:= :id id])
                  sql/format)]
    (jdbc/execute! (get-db) query)))
