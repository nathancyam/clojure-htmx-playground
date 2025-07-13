(ns user
  (:require [todo-app.db :as db]
            [todo-app.core :as core]))

;; REPL helpers for development
(defn reset-db! []
  "Reset the database connection"
  (db/init-db!)
  (db/setup-database!))

(defn sample-todos []
  "Create some sample todos for testing"
  (db/create-todo! {:title "Learn Clojure" :description "Study Clojure fundamentals"})
  (db/create-todo! {:title "Build web app" :description "Create a todo app with Ring"})
  (db/create-todo! {:title "Deploy to production" :description "Set up deployment pipeline"}))

(defn start-server []
  "Start the development server"
  (core/-main))
