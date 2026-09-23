(ns todo-app.db-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [support.db :refer [*db* with-test-db with-rollback]]
            [todo-app.db :as todos]))

(use-fixtures :once with-test-db)
(use-fixtures :each with-rollback)

(deftest create-todo!-test
  (let [todo (todos/create-todo! *db* {:title "Buy milk" :completed false})]
    (is (uuid? (:todos/id todo)))
    (is (= "Buy milk" (:todos/title todo)))
    (is (false? (:todos/completed todo)))
    (is (some? (:todos/created_at todo)))))

(deftest get-todo-by-id-test
  (let [{:todos/keys [id]} (todos/create-todo! *db* {:title "Find me"})]
    (testing "returns the todo when it exists"
      (is (= "Find me" (:todos/title (todos/get-todo-by-id *db* id)))))
    (testing "returns nil when it doesn't"
      (is (nil? (todos/get-todo-by-id *db* (random-uuid)))))))

(deftest get-all-todos-test
  (todos/create-todo! *db* {:title "One"})
  (todos/create-todo! *db* {:title "Two"})
  (is (= #{"One" "Two"}
         (set (map :todos/title (todos/get-all-todos *db*))))))

(deftest update-todo!-test
  (let [{:todos/keys [id]} (todos/create-todo! *db* {:title "Old" :completed false})
        updated (todos/update-todo! *db* id {:title "New" :completed true})]
    (is (= "New" (:todos/title updated)))
    (is (true? (:todos/completed updated)))
    (is (= updated (todos/get-todo-by-id *db* id)))))

(deftest delete-todo!-test
  (let [{:todos/keys [id]} (todos/create-todo! *db* {:title "Doomed"})]
    (todos/delete-todo! *db* id)
    (is (nil? (todos/get-todo-by-id *db* id)))))
