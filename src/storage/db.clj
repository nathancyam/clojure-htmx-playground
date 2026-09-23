(ns storage.db
  (:require [next.jdbc.connection :as connection]
            [next.jdbc.result-set :as rs]
            [clojure.tools.logging :as log]
            [aero.core :refer [read-config]])
  (:import (com.zaxxer.hikari HikariDataSource)
           (org.postgresql.util PGobject)))

;; The Postgres driver returns types it doesn't know, like citext, as
;; PGobject. Read citext as a plain string.
(defn- read-pgobject [^PGobject v]
  (if (= "citext" (.getType v))
    (.getValue v)
    v))

(extend-protocol rs/ReadableColumn
  PGobject
  (read-column-by-label [v _] (read-pgobject v))
  (read-column-by-index [v _ _] (read-pgobject v)))

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
