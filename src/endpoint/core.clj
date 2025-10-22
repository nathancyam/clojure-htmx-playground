(ns endpoint.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [clojure.tools.logging :as log]
            [storage.db :as db]
            [compojure.core :refer [defroutes context]]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.core.async :refer [go <!! >!! chan]]
            [todo-app.handler :as todos]))

(defroutes all-routes
  (context "/" [] todos/routes))

(def app
  (-> all-routes
      (wrap-json-body {:keywords? false})
      wrap-params
      wrap-json-response))

(defonce signal (chan))

(defonce dev-server (atom nil))

(defn start-dev-server []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (log/info "Starting server in development mode...")
    (db/init-db!)
    (log/info "Starting server on port" {:port port})
    (let [server (run-jetty (wrap-reload #'app) {:port port :join? false})]
      (reset! dev-server server)
      (server))))

(defn reload-dev-server []
  (.stop @dev-server)
  (start-dev-server))

(defn -main []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (db/init-db!)
    (log/info "Starting server on port" {:port port})
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. (fn []
                (log/info "Sending exit signal")
                (>!! signal :shutdown))))
    (let [server (run-jetty app {:port port :join? false})]
      (go
        (log/info "Started server on port " port " and waiting for shutdown signal...")
        (<!! signal)
        (println "Shutting down server...")
        (.stop server)
        (println "Closing database connections...")
        (db/close)
        (println "Server stopped.")))))
