(ns endpoint.core-test
  (:require [clojure.test :refer [deftest is]]
            [endpoint.core :as core]
            [ring.mock.request :as mock]))

(deftest healthz-test
  (let [response (core/app (mock/request :get "/healthz"))]
    (is (= 200 (:status response)))
    (is (= "ok" (:body response)))))
