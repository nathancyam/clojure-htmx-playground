(ns todo-app.handler-test
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [endpoint.core :as core]
            [ring.mock.request :as mock]
            [support.db :refer [*db* with-test-db with-rollback]]
            [todo-app.db :as todos]))

(use-fixtures :once with-test-db)
(use-fixtures :each with-rollback)

(defn- request
  "Send `req` through the full app, using the current test's transaction."
  [req]
  ((core/wrap-db core/app *db*) req))

(defn- json-body [response]
  (json/parse-string (:body response)))

(defn- todo-count []
  (count (todos/get-all-todos *db*)))

(defn- create-todo [title]
  (todos/create-todo! *db* {:title title :completed false}))

(def ^:private missing-id "00000000-0000-0000-0000-000000000000")

(deftest list-todos-test
  (create-todo "Walk the dog")
  (let [response (request (mock/request :get "/todos"))]
    (is (= 200 (:status response)))
    (is (= "text/html; charset=utf-8" (get-in response [:headers "Content-Type"])))
    (is (str/starts-with? (:body response) "<!DOCTYPE html>"))
    (is (str/includes? (:body response) "Walk the dog"))))

(deftest new-todo-test
  (testing "creates the todo and returns the list fragment"
    (let [response (request (mock/request :post "/todo" {:title "Water plants"}))]
      (is (= 200 (:status response)))
      (is (str/includes? (:body response) "Water plants"))
      (is (not (str/includes? (:body response) "<!DOCTYPE")))
      (is (= 1 (todo-count)))))

  (testing "rejects blank titles without creating anything"
    (doseq [params [{:title ""} {:title "   "} {}]]
      (let [response (request (mock/request :post "/todo" params))]
        (is (= 400 (:status response)) (str "params: " params))))
    (is (= 1 (todo-count)))))

(deftest toggle-todo-test
  (let [{:todos/keys [id]} (create-todo "Toggle me")]
    (testing "flips completed and returns the row that replaces itself"
      (let [response (request (mock/request :patch (str "/todos/" id "/status")))]
        (is (= 200 (:status response)))
        (is (str/includes? (:body response) "checked"))
        (is (str/includes? (:body response) (str "hx-target=\"#todo-" id "\"")))
        (is (true? (:todos/completed (todos/get-todo-by-id *db* id))))))

    (testing "flips it back"
      (request (mock/request :patch (str "/todos/" id "/status")))
      (is (false? (:todos/completed (todos/get-todo-by-id *db* id))))))

  (testing "404 for a missing todo"
    (is (= 404 (:status (request (mock/request :patch (str "/todos/" missing-id "/status")))))))

  (testing "400 for a malformed id"
    (is (= 400 (:status (request (mock/request :patch "/todos/garbage/status")))))))

(deftest get-todo-test
  (let [{:todos/keys [id]} (create-todo "Read me")
        response (request (mock/request :get (str "/todos/" id)))]
    (is (= 200 (:status response)))
    (is (= "Read me" (get (json-body response) "todos/title"))))

  (is (= 404 (:status (request (mock/request :get (str "/todos/" missing-id))))))
  (is (= 400 (:status (request (mock/request :get "/todos/garbage"))))))

(deftest update-todo-test
  (let [{:todos/keys [id]} (create-todo "Before")]
    (testing "updates the given fields"
      (let [response (request (-> (mock/request :put (str "/todos/" id))
                                  (mock/json-body {:title "After" :completed true})))]
        (is (= 200 (:status response)))
        (is (= "After" (get (json-body response) "todos/title")))
        (is (true? (get (json-body response) "todos/completed")))))

    (testing "400 when no known fields are given"
      (is (= 400 (:status (request (-> (mock/request :put (str "/todos/" id))
                                       (mock/json-body {:colour "red"}))))))))

  (testing "404 for a missing todo"
    (is (= 404 (:status (request (-> (mock/request :put (str "/todos/" missing-id))
                                     (mock/json-body {:title "x"})))))))

  (testing "400 for a malformed id"
    (is (= 400 (:status (request (-> (mock/request :put "/todos/garbage")
                                     (mock/json-body {:title "x"}))))))))

(deftest delete-todo-test
  (let [{:todos/keys [id]} (create-todo "Delete me")]
    (is (= 200 (:status (request (mock/request :delete (str "/todos/" id))))))
    (is (nil? (todos/get-todo-by-id *db* id)))
    (is (= 404 (:status (request (mock/request :delete (str "/todos/" id))))))
    (is (= 400 (:status (request (mock/request :delete "/todos/garbage")))))))

(deftest not-found-test
  (is (= 404 (:status (request (mock/request :get "/")))))
  (is (= 404 (:status (request (mock/request :get "/no/such/page"))))))
