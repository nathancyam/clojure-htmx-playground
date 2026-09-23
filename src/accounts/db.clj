(ns accounts.db
  (:require [honey.sql :as sql]
            [honey.sql.helpers :refer [select from where insert-into values returning order-by on-conflict do-nothing]]
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

(defn create-user!
  "Insert a user, hashing their password. Throws ex-info with
  `{:type :accounts/email-taken}` if the email is already registered."
  [db user-data]
  (let [query (-> (insert-into :users)
                  (values [(-> user-data
                               (assoc :hashed_password (password/encrypt (:password user-data)))
                               (dissoc :password)
                               (assoc :inserted_at [:now])
                               (assoc :updated_at [:now]))])
                  ;; DO NOTHING returns no row on a duplicate instead of raising,
                  ;; so a surrounding transaction isn't aborted.
                  (on-conflict :email)
                  (do-nothing)
                  (returning :*)
                  sql/format)]
    (or (first (jdbc/execute! db query))
        (throw (ex-info "Email is already registered"
                        {:type :accounts/email-taken
                         :email (:email user-data)})))))
