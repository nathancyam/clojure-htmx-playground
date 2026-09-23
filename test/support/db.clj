(ns support.db
  "Test database support. Tests assume todoapp_test exists and is migrated:
  run `clj -X:test-migrate` first, and again after adding migrations.
  `with-rollback` runs each test in a transaction that is rolled back, so
  tests never leave data behind."
  (:require [next.jdbc :as jdbc]
            [ragtime.next-jdbc :as ragtime]
            [ragtime.repl :as ragtime-repl]
            [storage.db :as db])
  (:import (java.sql SQLException)))

(def test-db-spec
  (assoc db/db-spec :dbname "todoapp_test"))

(def ^:dynamic *db*
  "The current test's transaction. Pass this wherever a db is expected."
  nil)

(defn- database-exists? [admin dbname]
  (some? (jdbc/execute-one! admin ["SELECT 1 FROM pg_database WHERE datname = ?" dbname])))

(defn migrate!
  "Create the test database if it's missing, then apply pending migrations.
  Run with `clj -X:test-migrate`."
  [_opts]
  ;; Hikari takes :username but a plain next.jdbc datasource wants :user.
  (let [admin (jdbc/get-datasource (assoc test-db-spec
                                          :dbname "postgres"
                                          :user (:username test-db-spec)))
        dbname (:dbname test-db-spec)]
    (when-not (database-exists? admin dbname)
      (println "Creating database" dbname)
      (jdbc/execute! admin [(str "CREATE DATABASE " dbname)])))
  (let [pool (db/make-pool test-db-spec)]
    (try
      (ragtime-repl/migrate {:datastore (ragtime/sql-database pool)
                             :migrations (ragtime/load-resources "migrations")})
      (finally
        (db/close pool)))))

;; One pool for the whole run, created on first use. A delay is safe if
;; tests run in parallel, and defonce keeps REPL reloads from leaking pools.
(defonce ^:private pool
  (delay (db/make-pool test-db-spec)))

(defn- missing-database? [e]
  (some #(and (instance? SQLException %) (= "3D000" (.getSQLState ^SQLException %)))
        (take-while some? (iterate ex-cause e))))

(defn with-rollback
  "`:each` fixture: run the test in a transaction that is always rolled back."
  [f]
  (try
    (jdbc/with-transaction [tx @pool {:rollback-only true}]
      (binding [*db* tx]
        (f)))
    (catch Exception e
      ;; No cause attached: the CLI reports an error's root cause, which
      ;; would hide this message behind the Postgres one.
      (if (missing-database? e)
        (throw (ex-info (str "Test database " (:dbname test-db-spec) " doesn't exist. "
                             "Run `clj -X:test-migrate` first.")
                        {:dbname (:dbname test-db-spec)
                         :error (ex-message e)}))
        (throw e)))))
