(ns accounts.db
  (:require [honey.sql :as sql]
            [honey.sql.helpers :refer [select from where insert-into values returning order-by]]
            [crypto.password.bcrypt :as password]
            [next.jdbc :as jdbc]))

(defn get-all-users
  [db]
  (let [query (-> (select :*)
                  (from :users)
                  (order-by [:inserted_at :desc])
                  sql/format)]
    (jdbc/execute! db query)))

(defn get-user-by-email
  "Get a user by their email address."
  [db email]
  (let [query (-> (select :*)
                  (from :users)
                  ;; JDBC sends a varchar, which would make Postgres compare as
                  ;; text; cast so the citext column matches case-insensitively.
                  (where [:= :email [:cast email :citext]])
                  sql/format)]
    (first (jdbc/execute! db query))))

(defn create-user! [db user-data]
  (let [query (-> (insert-into :users)
                  (values [(-> user-data
                               (assoc :hashed_password (password/encrypt (:password user-data)))
                               (dissoc :password)
                               (assoc :inserted_at [:now])
                               (assoc :updated_at [:now]))])
                  (returning :*)
                  sql/format)]
    (try
      (first (jdbc/execute! db query))
      (catch Exception e
        {:failed (.getMessage e)}))))
