(ns todo-app.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [todo-app.handler :refer [app]]
            [clojure.core.async :refer [go <!! >!! chan]]))

(defonce signal (chan))

(defn -main [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
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
        (println "Server stopped.")))))
