(ns storage.db
  (:require [next.jdbc.connection :as connection]
            [clojure.tools.logging :as log]
            [aero.core :refer [read-config]])
  (:import (com.zaxxer.hikari HikariDataSource)))

(def db-spec
  (-> (read-config "config.edn") :db-spec))

(defonce datasource (atom nil))

(defn init-db! []
  (log/info "Connecting to database...")
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
