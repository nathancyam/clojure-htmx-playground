(ns user
  (:require [accounts.db :as accounts]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [aero.core :refer [read-config]]
            [endpoint.core :as core]
            [clojure.core :as c]))

(def config
  {:datastore  (jdbc/sql-database {:connection-uri (-> (read-config "config.edn") :migrations :uri)})
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (repl/migrate config))

(defn dev-db
  "The dev server's connection pool, for running queries from the REPL."
  []
  (or (:db @core/dev-system)
      (throw (ex-info "Dev server isn't running; call (start-dev-server) first" {}))))

(defn sample-user
  [email]
  (accounts/create-user! (dev-db)
                         {:email email
                          :password "Password123!"}))

(defn start-dev-server
  "Start the development server"
  []
  (core/start-dev-server))

(defn stop-dev-server []
  (core/stop-dev-server))

(defn reload-dev-server []
  (core/reload-dev-server))
