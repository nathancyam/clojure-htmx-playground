(ns endpoint.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.reload :refer [wrap-reload]]
            [clojure.tools.logging :as log]
            [storage.db :as db]
            [compojure.core :refer [defroutes context]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [clojure.core.async :refer [go <!! >!! chan]]
            [todo-app.web :as todos]
            [accounts.web :as accounts]))

(defroutes all-routes
  (context "/" [] todos/routes)
  (context "/accounts" [] accounts/routes))

(def app
  (-> all-routes
      wrap-anti-forgery
      (wrap-session {:cookie-attrs {:http-only true :same-site :lax}})
      wrap-params))

(defonce signal (chan))

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
      (server))))

(defn reload-dev-server []
  (.stop @dev-server)
  (start-dev-server))

(defn -main []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))
        app (app)]
    (db/init-db!)
    (log/info "Starting server on port" {:port port})
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. (fn []
                (log/info "Sending exit signal")
                (>!! signal :shutdown))))
    (let [server (run-jetty (wrap-db app) {:port port})]
      (go
        (log/info "Started server on port " port " and waiting for shutdown signal...")
        (<!! signal)
        (println "Shutting down server...")
        (.stop server)
        (println "Closing database connections...")
        (db/close)
        (println "Server stopped.")))))
