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
    (is (fn? (sut/encode '(:x :y :z))))
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
