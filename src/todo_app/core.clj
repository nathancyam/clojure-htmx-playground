(ns todo-app.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [todo-app.db :as db]
            [todo-app.handler :refer [app]]
            [clojure.core.async :refer [go <!! >!! chan]]))

(defonce signal (chan))

(defn -main [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (println "Connecting to database...")
    (db/init-db!)
    (println (str "Starting server on port " port))
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. (fn []
                (println "Sending exit signal")
                (>!! signal :shutdown))))
    (let [server (run-jetty app {:port port :join? false})]
      (go
        (println "Started server on port " port " and waiting for shutdown signal...")
        (<!! signal)
        (println "Shutting down server...")
        (.stop server)
        (println "Closing database connections...")
        (db/close)
        (println "Server stopped.")))))
