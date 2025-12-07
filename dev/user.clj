(ns user
  (:require
   [aero.core :refer [read-config]]
   [clojure.core :as c]
   [endpoint.core :as core]
   [ragtime.jdbc :as jdbc]
   [ragtime.repl :as repl]))

(def config
  {:datastore  (jdbc/sql-database {:connection-uri (-> (read-config "config.edn") :migrations :uri)})
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (repl/migrate config))

(defn start-dev-server
  "Start the development server"
  []
  (core/start-dev-server))

(defn reload-dev-server []
  (core/reload-dev-server))
