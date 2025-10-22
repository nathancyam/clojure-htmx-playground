(ns accounts.db
  (:require [honey.sql :as sql]
            [honey.sql.helpers :refer [select from where insert-into values returning order-by]]
            [storage.db :refer [get-db]]
            [crypto.password.bcrypt :as password]
            [next.jdbc :as jdbc]))

(defn get-all-users
  []
  (let [query (-> (select :*)
                  (from :users)
                  (order-by [:inserted_at :desc])
                  sql/format)]
    (jdbc/execute! (get-db) query)))

(defn get-user-by-email
  "Get a user by their email address."
  [email]
  (let [query (-> (select :*)
                  (from :users)
                  (where [:= :email email])
                  sql/format)]
    (first (jdbc/execute! (get-db) query))))

(defn create-user! [user-data]
  (let [query (-> (insert-into :users)
                  (values [(-> user-data
                               (assoc :hashed_password (password/encrypt (:password user-data)))
                               (dissoc :password)
                               (assoc :inserted_at [:now])
                               (assoc :updated_at [:now]))])
                  (returning :*)
                  sql/format)]
    (try
      (first (jdbc/execute! (get-db) query))
      (catch Exception e
        {:failed (.getMessage e)}))))
