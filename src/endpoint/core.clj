(ns endpoint.core
  (:require
   [accounts.web :as accounts]
   [aero.core :refer [read-config]]
   [clojure.core.async :refer [<!! >!! chan go]]
   [clojure.tools.logging :as log]
   [compojure.core :refer [context defroutes]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.nested-params :refer [wrap-nested-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.session.cookie :refer [cookie-store]]
   [storage.db :as db]
   [todo-app.web :as todos]
   [web.response :refer [hx-response page-response]])
  (:import
   [java.util Base64]))

(defroutes all-routes
  (context "/" [] todos/routes)
  (context "/accounts" [] accounts/routes))

(def wrap-hx-header-check
  (fn [handler]
    (fn [request]
      (if (contains? (:headers request) "hx-request")
        (handler (assoc request :hx-request? true :renderer hx-response))
        (handler (assoc request :renderer page-response))))))

(defn wrap-db [handler]
  (fn [request]
    (let [db-conn (db/get-db)
          request-with-db (assoc request :db db-conn)]
      (handler request-with-db))))

(defn app [config]
  (let [secret (.decode (Base64/getDecoder) (:session-secret config))]
    (-> all-routes
        wrap-db
        wrap-hx-header-check
        wrap-anti-forgery
        wrap-keyword-params
        wrap-nested-params
        wrap-params
        (wrap-session {:store (cookie-store {:key (:session-secret secret)})
                       :cookie-attrs {:http-only true :same-site :lax}}))))

(defonce signal (chan))

(defonce dev-server (atom nil))

(defn start-dev-server []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (log/info "Starting server in development mode...")
    (db/init-db!)
    (log/info "Starting server on port" {:port port})
    (let [server (run-jetty (wrap-reload (app (read-config "config.edn"))) {:port port :join? false})]
      (reset! dev-server server)
      (server))))

(defn reload-dev-server []
  (.stop @dev-server)
  (start-dev-server))

(defn -main []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))
        _ (db/init-db!)
        app (app (read-config "config.edn"))]
    (log/info "Starting server on port" {:port port})
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. (fn []
                (log/info "Sending exit signal")
                (>!! signal :shutdown))))
    (let [server (run-jetty app {:port port})]
      (go
        (log/info "Started server on port " port " and waiting for shutdown signal...")
        (<!! signal)
        (println "Shutting down server...")
        (.stop server)
        (println "Closing database connections...")
        (db/close)
        (println "Server stopped.")))))
