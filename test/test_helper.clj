(ns test-helper
  (:require [next.jdbc.connection :as connection]
            [ring.mock :as mock]
            [ragtime.jdbc :as ragtime-jdbc]
            [ragtime.repl :as ragtime-repl])
  (:import (com.zaxxer.hikari HikariDataSource)))

(def db-spec
  {:dbtype "postgres"
   :dbname "todoapp_test"
   :host "localhost"
   :username "postgres"
   :password "postgres"})

(def test-pool (atom nil))

(defn setup-test-db []
  (let [ds (connection/->pool HikariDataSource db-spec)]
    (reset! test-pool ds)
    (ragtime-repl/migrate {:datastore (ragtime-jdbc/sql-database {:datasource ds})
                           :migrations (ragtime-jdbc/load-resources "migrations")})))

(defn teardown-test-db []
  (when-let [ds @test-pool]
    (.close ds)
    (reset! test-pool nil)))

(defn with-hx-headers [request]
  (-> request
      (mock/header "HX-Request" "true")
      (mock/header "HX-Boosted" "true")))

;; Run once on namespace load
(setup-test-db)

;; Shutdown hook for cleanup
(.addShutdownHook (Runtime/getRuntime)
                  (Thread. teardown-test-db))
