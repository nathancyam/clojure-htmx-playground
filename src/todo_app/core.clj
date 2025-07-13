(ns todo-app.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [todo-app.handler :refer [app]]))

(defn -main [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (println (str "Starting server on port " port))
    (run-jetty app {:port port :join? false})))
