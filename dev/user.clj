(ns user
  (:require [todo-app.db :as db]
            [todo-app.accounts :as accounts]
            [ragtime.jdbc :as jdbc]
            [crypto.password.bcrypt :as password]
            [todo-app.core :as core]))

(def config
  {:datastore  (jdbc/sql-database {:connection-uri "jdbc:postgresql://localhost:5432/todoapp_dev?user=postgres&password=postgres"})
   :migrations (jdbc/load-resources "migrations")})

(defn sample-todos
  "Create some sample todos for testing"
  []
  (db/create-todo! {:title "Learn Clojure" :description "Study Clojure fundamentals"})
  (db/create-todo! {:title "Build web app" :description "Create a todo app with Ring"})
  (db/create-todo! {:title "Deploy to production" :description "Set up deployment pipeline"}))

(defn sample-user
  [email]
  (accounts/create-user! {:email email
                          :password "Password123!"}))

(defn start-server
  "Start the development server"
  []
  (core/-main))
