(ns endpoint.core
  (:gen-class)
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [clojure.tools.logging :as log]
            [storage.db :as db]
            [storage.migrations :as migrations]
            [compojure.core :refer [defroutes context GET]]
            [compojure.route :as route]
            [ring.middleware.params :refer [wrap-params]]
            [todo-app.handler :as todos]))

(defroutes all-routes
  ;; Liveness check for Docker's HEALTHCHECK and load balancers: answers
  ;; while the server can handle requests. Deliberately doesn't touch the
  ;; database, so a database outage doesn't get the app restarted.
  (GET "/healthz" [] {:status 200 :headers {"Content-Type" "text/plain"} :body "ok"})
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

(defn- serve []
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

(defn- migrate []
  (let [pool (db/make-pool)]
    (try
      (migrations/migrate! pool)
      (finally
        (db/close pool)))))

(defn -main
  "With no arguments, start the web server. With `migrate`, apply pending
  database migrations and exit."
  [& args]
  (case (first args)
    nil       (serve)
    "migrate" (migrate)
    (binding [*out* *err*]
      (println "Unknown command:" (first args) "(expected no arguments, or migrate)")
      (System/exit 1))))
