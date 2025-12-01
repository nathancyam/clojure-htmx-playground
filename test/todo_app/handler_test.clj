(ns todo-app.handler-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [ring.mock.request :as mock]
            [next.jdbc.connection :as connection]
            [ragtime.next-jdbc :as ragtime-jdbc]
            [ragtime.repl :as ragtime-repl]
            [todo-app.handler :refer [routes]])
  (:import (com.zaxxer.hikari HikariDataSource)))

(def ^:dynamic *db* nil)

(def db-spec
  {:dbtype "postgres"
   :dbname "todoapp_test"
   :host "localhost"
   :username "postgres"
   :password "postgres"})

(defn setup-db [f]
  (let [ds (connection/->pool HikariDataSource db-spec)]
    (try
      (binding [*db* ds]
        ;; Run migrations/schema setup
        (ragtime-repl/migrate {:datastore (ragtime-jdbc/sql-database ds)
                               :migrations (ragtime-jdbc/load-resources "migrations")})
        (f))
      (finally
        (.close ds)))))

(use-fixtures :once setup-db)

(deftest test-additions
  (is (= (+ 1 2) 3))
  (is (= (+ -1 1) 0)))

(deftest test-todo-app-id-routes
  (let [response (routes (mock/request :post "/todo" {:title "Title"}))]
    (is (= 201 (:status response)))))
