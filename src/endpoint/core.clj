(ns endpoint.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [clojure.tools.logging :as log]
            [storage.db :as db]
            [compojure.core :refer [defroutes context]]
            [compojure.route :as route]
            [ring.middleware.params :refer [wrap-params]]
            [todo-app.handler :as todos]))

(defroutes all-routes
  (context "/" [] todos/routes)
  (route/not-found "Not found"))

(def app
  (-> all-routes
      (wrap-json-body {:keywords? false})
      wrap-params
      wrap-json-response))

(defonce dev-server (atom nil))

(defn wrap-db [handler]
  (fn [request]
    (let [db-conn (db/get-db)
          request-with-db (assoc request :db db-conn)]
      (handler request-with-db))))

(defn start-dev-server []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (log/info "Starting server in development mode...")
    (db/init-db!)
    (log/info "Starting server on port" {:port port})
    (let [server (run-jetty (wrap-reload (wrap-db #'app)) {:port port :join? false})]
      (reset! dev-server server)
      server)))

(defn reload-dev-server []
  (.stop @dev-server)
  (start-dev-server))

(defn -main []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (db/init-db!)
    (log/info "Starting server on port" {:port port})
    (let [server (run-jetty (wrap-db app) {:port port :join? false})]
      (.addShutdownHook
       (Runtime/getRuntime)
       (Thread. (fn []
                  (println "Shutting down server...")
                  (.stop server)
                  (println "Closing database connections...")
                  (db/close)
                  (println "Server stopped."))))
      (.join server))))
