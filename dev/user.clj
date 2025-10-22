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

(defn sample-user
  [email]
  (accounts/create-user! {:email email
                          :password "Password123!"}))

(defn start-dev-server
  "Start the development server"
  []
  (core/start-dev-server))

(defn reload-dev-server []
  (core/reload-dev-server))
