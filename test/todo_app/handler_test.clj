(ns todo-app.handler-test
  (:require [clojure.test :refer [deftest is]]))

(deftest test-additions
  (is (= (+ 1 2) 3))
  (is (= (+ -1 1) 0)))
