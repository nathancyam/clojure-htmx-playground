(ns todo-app.db
  (:require [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [storage.db :refer [get-db]]
            [honey.sql.helpers :as h :refer [select from order-by where insert-into values returning delete-from]]))

(defn now-timestamp []
  (java.sql.Timestamp/from (java.time.Instant/now)))

(defn with-update-timestamps [params]
  (into {:updated_at (now-timestamp)} params))

;; Database operations
(defn get-all-todos [db]
  (let [query (-> (select :*)
                  (from :todos)
                  (order-by [:updated_at :desc])
                  sql/format)]
    (jdbc/execute! db query)))

(defn get-todo-by-id [db id]
  (let [query (-> (select :*)
                  (from :todos)
                  (where [:= :id [:cast id :uuid]])
                  sql/format)]
    (first (jdbc/execute! db query))))

(defn create-todo! [db todo-data]
  (let [now (now-timestamp)
        with-timestamps (into {:created_at now :updated_at now} todo-data)
        query (-> (insert-into :todos)
                  (values [with-timestamps])
                  (returning :*)
                  sql/format)]
    (first (jdbc/execute! db query))))

(defn update-todo! [db id todo-data]
  (let [query (-> (h/update :todos)
                  (h/set (with-update-timestamps todo-data))
                  (where [:= :id id])
                  (returning :*)
                  sql/format)]
    (first (jdbc/execute! db query))))

(defn delete-todo! [db id]
  (let [query (-> (delete-from :todos)
                  (where [:= :id id])
                  sql/format)]
    (jdbc/execute! db query)))
