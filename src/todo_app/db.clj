(ns todo-app.db
  (:require [next.jdbc.connection :as connection]
            [aero.core :refer [read-config]])
  (:import (com.zaxxer.hikari HikariDataSource)))

(def db-spec
  (-> (read-config "config.edn") :db-spec))

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
