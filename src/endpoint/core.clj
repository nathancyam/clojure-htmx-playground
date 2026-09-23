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

(defonce dev-system (atom nil))

(defn wrap-db [handler db]
  (fn [request]
    (handler (assoc request :db db))))

(defn start-dev-server []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))
        pool (db/make-pool)]
    (log/info "Starting server in development mode...")
    (log/info "Starting server on port" {:port port})
    (let [server (run-jetty (wrap-reload (wrap-db #'app pool)) {:port port :join? false})]
      (reset! dev-system {:server server :db pool})
      server)))

(defn stop-dev-server []
  (when-let [{:keys [server db]} @dev-system]
    (.stop server)
    (db/close db)
    (reset! dev-system nil)))

(defn reload-dev-server []
  (stop-dev-server)
  (start-dev-server))

(defn -main []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))
        pool (db/make-pool)]
    (log/info "Starting server on port" {:port port})
    (let [server (run-jetty (wrap-db app pool) {:port port :join? false})]
      (.addShutdownHook
       (Runtime/getRuntime)
       (Thread. (fn []
                  (println "Shutting down server...")
                  (.stop server)
                  (println "Closing database connections...")
                  (db/close pool)
                  (println "Server stopped."))))
      (.join server))))
