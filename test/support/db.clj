(ns support.db
  "Test fixtures. `with-test-db` creates and migrates the todoapp_test
  database once per namespace; `with-rollback` runs each test inside a
  transaction that is rolled back, so tests never leave data behind."
  (:require [next.jdbc :as jdbc]
            [ragtime.next-jdbc :as ragtime]
            [ragtime.repl :as ragtime-repl]
            [storage.db :as db]))

(def test-db-spec
  (assoc db/db-spec :dbname "todoapp_test"))

(def ^:dynamic *pool* nil)

(def ^:dynamic *db*
  "The current test's transaction. Pass this wherever a db is expected."
  nil)

(defn- ensure-database! []
  ;; Hikari takes :username but a plain next.jdbc datasource wants :user.
  (let [admin (jdbc/get-datasource (assoc test-db-spec
                                          :dbname "postgres"
                                          :user (:username test-db-spec)))
        dbname (:dbname test-db-spec)]
    (when-not (jdbc/execute-one! admin ["SELECT 1 FROM pg_database WHERE datname = ?" dbname])
      (jdbc/execute! admin [(str "CREATE DATABASE " dbname)]))))

(defn with-test-db
  "`:once` fixture: create and migrate the test database, and open a pool."
  [f]
  (ensure-database!)
  (let [pool (db/make-pool test-db-spec)]
    (try
      (ragtime-repl/migrate {:datastore (ragtime/sql-database pool)
                             :migrations (ragtime/load-resources "migrations")
                             :reporter (constantly nil)})
      (binding [*pool* pool]
        (f))
      (finally
        (db/close pool)))))

(defn with-rollback
  "`:each` fixture: run the test in a transaction that is always rolled back."
  [f]
  (jdbc/with-transaction [tx *pool* {:rollback-only true}]
    (binding [*db* tx]
      (f))))
