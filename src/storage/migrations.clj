(ns storage.migrations
  (:require [ragtime.next-jdbc :as ragtime]
            [ragtime.repl :as ragtime-repl]))

(defn migrate!
  "Apply pending migrations from resources/migrations to `datasource`."
  [datasource]
  (ragtime-repl/migrate {:datastore  (ragtime/sql-database datasource)
                         :migrations (ragtime/load-resources "migrations")}))
