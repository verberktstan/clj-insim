(ns clj-insim.enum-test
  (:require [clj-insim.enum :as sut]
            [clojure.test :refer [deftest testing is are]]))

(deftest encode-test
  (testing "encode"
    (let [encode (sut/encode [:a :b :c])]
      (are [x y] (= x y)
        0 (encode :a)
        1 (encode :b)
        nil (encode :z))))
  (testing "accepts only sequential enum"
    (is (ifn? (sut/encode '(:x :y :z))))
    (is (thrown? AssertionError (sut/encode {:test "map"})))))

(deftest decode-test
  (testing "decode"
    (let [decode (sut/decode [:a :b :c])]
      (are [x y] (= x y)
        :a (decode 0)
        :b (decode 1)
        nil (decode 100))))
  (testing "accepts only sequential enum"
    (is (fn? (sut/decode '(:x :y :z))))
    (is (thrown? AssertionError (sut/decode {:test "map"})))))

;; Phase 1: InSim v10 Enum Updates
(deftest insim-v10-tiny-header-data-test
  (testing "TINY_HEADER_DATA - v10 changes"
    (testing "position 8 renamed GTH → GTM"
      (is (= :gtm (nth sut/TINY_HEADER_DATA 8))))
    (testing "position 28: TINY_PLH added"
      (is (= :plh (nth sut/TINY_HEADER_DATA 28))))
    (testing "position 29: TINY_IPB added"
      (is (= :ipb (nth sut/TINY_HEADER_DATA 29))))
    (testing "position 30: TINY_LCL added"
      (is (= :lcl (nth sut/TINY_HEADER_DATA 30))))))

(deftest insim-v10-small-header-data-test
  (testing "SMALL_HEADER_DATA - v10 changes"
    (testing "position 10: SMALL_LCL added"
      (is (= :lcl (nth sut/SMALL_HEADER_DATA 10))))))

(deftest insim-v10-header-type-test
  (testing "HEADER_TYPE - v10 changes"
    (testing "position 66: ISP_PLH added"
      (is (= :plh (nth sut/HEADER_TYPE 66))))))
