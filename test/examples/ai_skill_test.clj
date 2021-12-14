(ns examples.ai-skill-test
  (:require [clojure.test :refer [are deftest is testing]]
            [examples.ai-skill :as sut]))

(deftest grid-skills-test
  (testing "Returns a vector of skills for a number of racers on the grid"
    (is (= (#'sut/grid-skills 3) [5 4 3]))
    (is (= (#'sut/grid-skills 5) [5 4 3 2 1]))
    (is (= (#'sut/grid-skills 8) [5 5 4 4 3 3 2 2]))
    (is (= (#'sut/grid-skills 13) [5 5 5 4 4 4 3 3 3 2 2 2 1]))
    (is (= (#'sut/grid-skills 21) [5 5 5 5 5 4 4 4 4 4 3 3 3 3 3 2 2 2 2 2 1]))))

(deftest gravitate-test
  (testing "gravitates x towards target"
    (are [y x t] (= y (#'sut/gravitate x t))
      5 4 5
      2 1 5
      2 3 1
      2 3 2)))
