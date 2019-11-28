(ns clj-insim.core-test
  (:require [clj-insim.core :as sut]
            [clojure.test :as t]))

(t/deftest parse-header-test
  (t/testing "It parses a tiny type header"
    (t/is (= (sut/parse-header [4 3 1 0])
             {:size 4 :type :tiny :request-info 1 :subtype :none}))
    (t/is (= (sut/parse-header [4 3 0 1])
             {:size 4 :type :tiny :request-info 0 :subtype :ver}))))
