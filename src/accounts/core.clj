(ns accounts.core
  (:require [honey.sql :as sql]
            [honey.sql.helpers :refer [select from where insert-into join values returning delete-from]]
            [crypto.password.bcrypt :as password]
            [next.jdbc :as jdbc]))

(defn- random-bytes [n]
  (let [b (byte-array n)]
    (.nextBytes (java.security.SecureRandom.) b)
    b))

(defn generate-user-token [db user context]
  (let [token (random-bytes 32)
        query (-> (insert-into :users_tokens)
                  (values [{:user_id (:users/id user)
                            :token token
                            :context context
                            :inserted_at [:now]}])
                  (returning :*)
                  sql/format)]
    (jdbc/execute! db query)
    token))

(defn get-user-by-email
  "Get a user by their email address."
  [db email]
  (let [query (-> (select :*)
                  (from :users)
                  (where [:= :email email])
                  sql/format)]
    (when-let [user (first (jdbc/execute! db query))]
      (update user :users/email #(.getValue %)))))

(defn get-user-by-session-token [db token]
  (let [query (-> (select [:u.*])
                  (from [:users_tokens :ut])
                  (where [:= :ut.token token]
                         [:= :ut.context "session"])
                  (join [:users :u] [:= :u.id :ut.user_id])
                  sql/format)]
    (when-let [user (first (jdbc/execute! db query))]
      (update user :users/email #(.getValue %)))))

(defn decode-token [base-encoded-token]
  (.decode (java.util.Base64/getDecoder) base-encoded-token))

(defn delete-user-token [db token]
  (let [query (-> (delete-from :users_tokens)
                  (where [:= :token token]
                         [:= :context "session"])
                  sql/format)]
    (jdbc/execute! db query)))

(defn authenticate! [db email password]
  (let [user (get-user-by-email db email)
        _ (when-not user
            (throw (ex-info "Could not find user with that email"
                            {:reason :not-found})))

        valid? (password/check password (:users/hashed_password user))
        _ (when-not valid?
            (throw (ex-info "Your password is incorrect"
                            {:reason :invalid-password})))

        token (generate-user-token db user "session")
        _ (when-not token
            (throw (ex-info "Could not generate user token"
                            {:reason :token-generation-failed})))]
    {:user user :token token}))

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
