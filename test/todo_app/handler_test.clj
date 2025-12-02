(ns todo-app.handler-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [ring.mock.request :as mock]
            [next.jdbc :as jdbc]
            [ring.middleware.params :refer [wrap-params]]
            [test-helper :as helper]
            [ring.util.codec :as codec]
            [todo-app.handler :refer [routes]]))

(def ^:dynamic *tx* nil)

(defn with-rollback [f]
  (jdbc/with-transaction [tx @helper/test-pool {:rollback-only true}]
    (binding [*tx* tx]
      (f))))

(use-fixtures :each with-rollback)

(defn wrap-db [handler]
  (fn [request]
    (let [request-with-db (assoc request :db *tx*)]
      (handler request-with-db))))

(deftest test-additions
  (is (= (+ 1 2) 3))
  (is (= (+ -1 1) 0)))

(deftest test-todo-app-id-routes
  (let [handler (-> routes
                    wrap-params
                    wrap-db)
        response (handler (-> (mock/request :post "/todo")
                              (mock/body (codec/form-encode {:title "Test"}))
                              (mock/content-type "application/x-www-form-urlencoded")))]
    (is (= 200 (:status response))))
  (let [todos (jdbc/execute! *tx* ["SELECT * FROM todos WHERE title = ?" "Test"])]
    (is (= "Test" (:todos/title (first todos))))))
