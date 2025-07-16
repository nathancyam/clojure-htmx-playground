(ns todo-app.db
  (:require [honey.sql :as sql]
            [honey.sql.helpers :as h]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource)))

(def db-spec
  {:dbtype "postgres"
   :dbname "todoapp_dev"
   :host "localhost"
   :username "postgres"
   :password "postgres"})

(defonce datasource (atom nil))

(defn init-db! []
  (when @datasource
    (.close @datasource))
  (reset! datasource (connection/->pool HikariDataSource db-spec)))

(defn get-db []
  (when-not @datasource
    (init-db!))
  {:datasource @datasource})

(defn close []
  (when @datasource
    (.close @datasource)))

;; Database operations
(defn get-all-todos []
  (let [query (-> (h/select :*)
                  (h/from :todos)
                  (h/order-by [:created_at :desc])
                  sql/format)]
    (jdbc/execute! (get-db) query)))

(defn get-todo-by-id [id]
  (let [query (-> (h/select :*)
                  (h/from :todos)
                  (h/where [:= :id id])
                  sql/format)]
    (first (jdbc/execute! (get-db) query))))

(defn create-todo! [todo-data]
  (let [query (-> (h/insert-into :todos)
                  (h/values [todo-data])
                  (h/returning :*)
                  sql/format)]
    (first (jdbc/execute! (get-db) query))))

(defn update-todo! [id todo-data]
  (let [query (-> (h/update :todos)
                  (h/set todo-data)
                  (h/where [:= :id id])
                  (h/returning :*)
                  sql/format)]
    (first (jdbc/execute! (get-db) query))))

(defn delete-todo! [id]
  (let [query (-> (h/delete-from :todos)
                  (h/where [:= :id id])
                  sql/format)]
    (jdbc/execute! (get-db) query)))
