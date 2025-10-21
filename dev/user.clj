(ns user
  (:require [todo-app.accounts :as accounts]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [todo-app.todos :as todos]
            [aero.core :refer [read-config]]
            [todo-app.core :as core]
            [clojure.core :as c]))

(def config
  {:datastore  (jdbc/sql-database {:connection-uri (-> (read-config "config.edn") :migrations :uri)})
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (repl/migrate config))

(defn sample-todos
  "Create some sample todos for testing"
  []
  (todos/create-todo! {:title "Learn Clojure" :description "Study Clojure fundamentals"})
  (todos/create-todo! {:title "Build web app" :description "Create a todo app with Ring"})
  (todos/create-todo! {:title "Deploy to production" :description "Set up deployment pipeline"}))

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
