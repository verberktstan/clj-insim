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
        #{} (parse 0)
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
        0 (unparse #{})
        1 (unparse #{:a})
        2 (unparse #{:b})
        3 (unparse #{:a :b})
        4 (unparse #{:c})
        5 (unparse #{:a :c})
        6 (unparse #{:b :c})
        7 (unparse #{:a :b :c})))))

;; Phase 1: InSim v10 Flag Updates
(deftest insim-v10-player-flags-test
  (testing "PLAYER flags - v10 changes"
    (testing "position 5: reserved-32 → flexible-steer"
      (is (= :flexible-steer (nth sut/PLAYER 5))))))

(deftest insim-v10-car-contact-info-flags-test
  (testing "CAR_CONTACT_INFO flags - v10 new flag set"
    (testing "position 0: blue (CCI_BLUE)"
      (is (= :blue (nth sut/CAR_CONTACT_INFO 0))))
    (testing "position 1: yellow (CCI_YELLOW)"
      (is (= :yellow (nth sut/CAR_CONTACT_INFO 1))))
    (testing "position 2: oob (CCI_OOB) - NEW in v10"
      (is (= :oob (nth sut/CAR_CONTACT_INFO 2))))
    (testing "position 7: lag (CCI_LAG)"
      (is (= :lag (nth sut/CAR_CONTACT_INFO 7)))))
  (testing "CAR_CONTACT_INFO flag parsing/unparsing"
    (let [parse (sut/parse sut/CAR_CONTACT_INFO)
          unparse (sut/unparse sut/CAR_CONTACT_INFO)]
      (testing "can parse combined flags"
        (is (= #{:blue :yellow} (parse 3)))
        (is (= #{:blue :oob} (parse 5)))
        (is (= #{:lag} (parse 128))))
      (testing "can unparse back to value"
        (is (= 3 (unparse #{:blue :yellow})))
        (is (= 5 (unparse #{:blue :oob})))
        (is (= 128 (unparse #{:lag})))))))
