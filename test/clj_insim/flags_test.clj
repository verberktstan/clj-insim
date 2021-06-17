(ns clj-insim.flags-test
  (:require [clojure.test :refer [deftest testing is are]]
            [clj-insim.flags :as sut]))

(deftest power-range-test
  (testing "power-range"
    (is (= [1 2 4] (#'sut/power-range 3)))
    (is (= [1 2 4 8 16 32] (#'sut/power-range 6)))))

(deftest parse-test
  (testing "parse"
    (let [parse (sut/parse [:a :b :c])]
      (are [x y] (= x y)
        #{:a} (parse 1)
        #{:b} (parse 2)
        #{:a :b} (parse 3)
        #{:c} (parse 4)
        #{:a :c} (parse 5)
        #{:b :c} (parse 6)
        #{:a :b :c} (parse 7)))))

(deftest unparse-test
  (testing "unparse"
    (let [unparse (sut/unparse [:a :b :c])]
      (are [x y] (= x y)
        1 (unparse #{:a})
        2 (unparse #{:b})
        3 (unparse #{:a :b})
        4 (unparse #{:c})
        5 (unparse #{:a :c})
        6 (unparse #{:b :c})
        7 (unparse #{:a :b :c})))))
