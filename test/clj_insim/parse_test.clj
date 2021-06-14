(ns clj-insim.parse-test
  (:require [clojure.test :refer [deftest testing is]]
            [clj-insim.parse :as sut]))

(deftest header-test
  (testing "header"
    (testing "returns header with parsed `:header/type` and `:header/data`"
      (is (= #:header{:size 4 :type :tiny :request-info 0 :data :none}
             (sut/header
              #:header{:size 4 :type 3 :request-info 0 :data 0})))
      (is (= #:header{:size 4 :type :tiny :request-info 0 :data :close}
             (sut/header
              #:header{:size 4 :type 3 :request-info 0 :data 2}))))))

(deftest pipeline-test
  (testing "pipe unparse & parse"
    (let [packet (merge #:header{:size 4 :type :small :request-info 1 :data :ssp}
                        #:body{:update-interval 500})]
      (is (= packet
             ((comp sut/body sut/header) (sut/unparse packet)))))))
