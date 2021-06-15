(ns clj-insim.flags-test
  (:require [clojure.test :refer [deftest testing is are]]
            [clj-insim.flags :as sut]))

(deftest power-range-test
  (testing "power-range"
    (is (= [1 2 4] (#'sut/power-range 3)))
    (is (= [1 2 4 8 16 32] (#'sut/power-range 6)))))

(deftest parse-test
  (testing "parse"
    (let [coll [:a :b :c]]
      (are [x y] (= x y)
        #{:a} (sut/parse coll 1)
        #{:b} (sut/parse coll 2)
        #{:a :b} (sut/parse coll 3)
        #{:c} (sut/parse coll 4)
        #{:a :c} (sut/parse coll 5)
        #{:b :c} (sut/parse coll 6)
        #{:a :b :c} (sut/parse coll 7)))))

(deftest unparse-test
  (testing "unparse"
    (let [coll [:a :b :c]]
      (are [x y] (= x y)
        1 (sut/unparse coll #{:a})
        2 (sut/unparse coll #{:b})
        3 (sut/unparse coll #{:a :b})
        4 (sut/unparse coll #{:c})
        5 (sut/unparse coll #{:a :c})
        6 (sut/unparse coll #{:b :c})
        7 (sut/unparse coll #{:a :b :c})))))
