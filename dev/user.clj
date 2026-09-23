(ns user
  (:require [accounts.db :as accounts]
            [endpoint.core :as core]
            [storage.db :as db]
            [storage.migrations :as migrations]
            [clojure.core :as c]))

(defn migrate []
  (let [pool (db/make-pool)]
    (try
      (migrations/migrate! pool)
      (finally
        (db/close pool)))))

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
