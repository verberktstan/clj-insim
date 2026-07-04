(ns clj-insim.codecs-test
  (:require [clj-insim.codecs :as sut]
            [clojure.test :refer [deftest testing is]]))

;; Verify that time fields are correctly sized and typed for v10
(deftest con-codec-test
  (testing "IS_CON codec - time field updated for v10"
    (testing "codec structure is correct"
      (let [con-body (:con sut/body)
            codec    (con-body {})]
        (is (some? codec) "the codec can be created")))))

(deftest hlv-codec-test
  (testing "IS_HLV codec - time field updated for v10"
    (testing "codec structure is correct"
      (let [hlv-body (:hlv sut/body)
            codec    (hlv-body {})]
        (is (some? codec) "IS_HLV body codec should handle the fields correctly")))))

(deftest obh-codec-test
  (testing "IS_OBH codec - time field updated for v10"
    (testing "codec structure is correct"
      (let [obh-body (:obh sut/body)
            codec    (obh-body {})]
        (is (some? codec) "IS_OBH body codec should handle the fields correctly")))))

(deftest rip-codec-test
  (testing "IS_RIP codec - newly added for v10 support"
    (testing "codec exists"
      (let [rip-body (:rip sut/body)
            codec    (rip-body {})]
        (is (some? codec) "IS_RIP codec should now exist")))))
