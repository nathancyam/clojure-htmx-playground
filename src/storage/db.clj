(ns storage.db
  (:require [next.jdbc.connection :as connection]
            [clojure.tools.logging :as log]
            [aero.core :refer [read-config]])
  (:import (com.zaxxer.hikari HikariDataSource)))

(def db-spec
  (-> (read-config "config.edn") :db-spec))

(defn make-pool
  "Create a connection pool. The caller owns it and must `close` it."
  ([] (make-pool db-spec))
  ([spec]
   (log/info "Connecting to database...")
   (connection/->pool HikariDataSource spec)))

(defn close [^HikariDataSource pool]
  (when pool
    (.close pool)))
