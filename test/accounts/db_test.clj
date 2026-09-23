(ns accounts.db-test
  (:require [accounts.db :as accounts]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [crypto.password.bcrypt :as password]
            [support.db :refer [*db* with-rollback]]))

(use-fixtures :each with-rollback)

(deftest create-user!-test
  (let [user (accounts/create-user! *db* {:email "ada@example.com" :password "Password123!"})]
    (testing "stores a bcrypt hash, not the password"
      (is (not= "Password123!" (:users/hashed_password user)))
      (is (password/check "Password123!" (:users/hashed_password user))))
    (testing "sets timestamps"
      (is (some? (:users/inserted_at user)))
      (is (some? (:users/updated_at user))))))

(deftest get-user-by-email-test
  (accounts/create-user! *db* {:email "ada@example.com" :password "Password123!"})
  (testing "finds the user, ignoring case"
    (is (= "ada@example.com"
           (:users/email (accounts/get-user-by-email *db* "ADA@Example.com")))))
  (testing "returns nil for an unknown email"
    (is (nil? (accounts/get-user-by-email *db* "nobody@example.com")))))
