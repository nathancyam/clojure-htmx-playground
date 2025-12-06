(ns accounts.core-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [accounts.core :as accounts]
            [next.jdbc :as jdbc]
            [test-helper :as helper]))

(def ^:dynamic *tx* nil)

(defn with-rollback [f]
  (jdbc/with-transaction [tx @helper/test-pool {:rollback-only true}]
    (binding [*tx* tx]
      (f))))

(use-fixtures :each with-rollback)

(deftest test-create-user!
  (let [user (accounts/create-user! *tx* {:password "securepassword"
                                          :email "test@example.com"})]

    (is (some? (:users/id user)))
    (is (= "test@example.com" (.getValue (:users/email user))))
    (is (not= "securepassword" (:users/hashed_password user)))
    (is (some? (:users/inserted_at user)))))

(deftest test-generate-user-token
  (let [user (accounts/create-user! *tx* {:password "securepassword"
                                          :email "test@example.com"})
        token (accounts/generate-user-token *tx* user "login")]
    (is (some? token))
    (is (some? (:users_tokens/id token)))
    (is (some? (:users_tokens/token token)))
    (is (= "login" (:users_tokens/context token)))))
